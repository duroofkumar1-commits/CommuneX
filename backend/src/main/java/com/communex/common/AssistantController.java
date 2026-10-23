package com.communex.common;

import com.communex.facilities.FacilityService;
import com.communex.facilities.dto.FacilityResponse;
import com.communex.resources.ResourceService;
import com.communex.resources.dto.ResourceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final FacilityService facilities;
    private final ResourceService resources;
    private final ObjectMapper mapper;
    private final RestClient restClient;

    private final String aiBaseUrl;
    private final String aiModel;

    // Constructor
    public AssistantController(
            FacilityService facilities,
            ResourceService resources,
            ObjectMapper mapper,
            @Value("${communex.ai.base-url:}") String aiBaseUrl,
            @Value("${communex.ai.model:llama3.2}") String aiModel
    ) {
        this.facilities = facilities;
        this.resources = resources;
        this.mapper = mapper;
        this.aiBaseUrl = aiBaseUrl;
        this.aiModel = aiModel;
        this.restClient = RestClient.create();
    }

    // Main assistant chat endpoint
    @PostMapping("/chat")
    public ChatResponse chat(
            Authentication authentication,
            @Valid @RequestBody ChatRequest request
    ) {
        String message = request.message().trim();
        String lower = message.toLowerCase(Locale.ROOT);

        List<String> sources = new ArrayList<>();

        // Handle facility-related questions
        if (containsAny(
                lower,
                "facility",
                "facilities",
                "hospital",
                "clinic",
                "pharmacy",
                "shelter",
                "library"
        )) {
            return handleFacilityQuery(lower, sources);
        }

        // Handle resource and inventory questions
        if (containsAny(
                lower,
                "resource",
                "inventory",
                "stock",
                "supplies",
                "medicine",
                "medicines",
                "available goods"
        )) {
            return handleResourceQuery(sources);
        }

        // Handle help-request guidance
        if (containsAny(
                lower,
                "help request",
                "assistance request",
                "ask for help",
                "submit a request"
        )) {
            sources.add("CommuneX help-request workflow");

            return new ChatResponse(
                    "To request community assistance, open Help Requests, "
                            + "choose ‘Create request’, describe the need and "
                            + "location, then submit. You can track status "
                            + "under My Requests. CommuneX is not an emergency "
                            + "dispatch service; contact official local "
                            + "emergency services for genuine emergencies.",
                    sources
            );
        }

        // Handle volunteer-related questions
        if (containsAny(lower, "volunteer", "volunteering")) {
            sources.add("CommuneX volunteer workflow");

            return new ChatResponse(
                    "Open your dashboard and use the volunteer registration "
                            + "form to add your skills and a short bio. Once "
                            + "your profile is registered, you can update "
                            + "availability and browse open assistance "
                            + "requests. Request acceptance and completion "
                            + "are checked by the backend.",
                    sources
            );
        }

        // Handle map and navigation questions
        if (containsAny(
                lower,
                "route",
                "directions",
                "shortest path",
                "navigate"
        )) {
            sources.add("CommuneX map explorer");

            return new ChatResponse(
                    "Open Map Explorer and select two facility records "
                            + "that include coordinates. The route viewer "
                            + "uses the configured public routing demo "
                            + "provider. If coordinates or a route are "
                            + "unavailable, I won't invent a route. Do not "
                            + "use a demo route as emergency navigation.",
                    sources
            );
        }

        // Use the optional local language model for general questions
        String answer = callOptionalLocalModel(message);

        if (answer != null && !answer.isBlank()) {
            return new ChatResponse(
                    answer,
                    List.of("Locally configured Ollama model: " + aiModel)
            );
        }

        // Default response when no language model is available
        return new ChatResponse(
                "I can help you navigate CommuneX and search its current "
                        + "facility/resource records. For general AI chat, "
                        + "configure a local Ollama model using "
                        + "COMMUNEX_AI_BASE_URL and COMMUNEX_AI_MODEL; no "
                        + "hosted paid API is required by default. Available "
                        + "platform tools include facility lookup, inventory "
                        + "lookup, help-request guidance, volunteer guidance "
                        + "and map guidance. I don't have a configured "
                        + "language model for general questions in this deployment.",
                List.of("CommuneX Assistant configuration")
        );
    }

    // Search and format matching facility records
    private ChatResponse handleFacilityQuery(
            String message,
            List<String> sources
    ) {
        String category = null;

        if (message.contains("hospital")) {
            category = "Hospital";
        } else if (message.contains("clinic")) {
            category = "Clinic";
        } else if (message.contains("pharmacy")) {
            category = "Pharmacy";
        }

        List<FacilityResponse> found = facilities.search(null, category)
                .stream()
                .limit(5)
                .toList();

        sources.add("CommuneX facility directory");

        if (found.isEmpty()) {
            return new ChatResponse(
                    "I couldn't find matching facility records in the "
                            + "current CommuneX database. The directory may "
                            + "not yet contain verified local data. For a "
                            + "genuine emergency, contact official local "
                            + "emergency services.",
                    sources
            );
        }

        String records = found.stream()
                .map(this::formatFacility)
                .collect(Collectors.joining("\n"));

        return new ChatResponse(
                "These are the matching facility records currently listed "
                        + "in CommuneX:\n" + records,
                sources
        );
    }

    // Format a single facility record
    private String formatFacility(FacilityResponse facility) {

        String address = facility.address();

        if (address == null) {
            address = "address unavailable";
        }

        String verificationStatus;

        if (facility.verified()) {
            verificationStatus = "verified platform record";
        } else {
            verificationStatus = "unverified submission";
        }

        return "• " + facility.name()
                + " — " + address
                + " [" + verificationStatus + "]";
    }

    // Search and format resource inventory records
    private ChatResponse handleResourceQuery(List<String> sources) {

        List<ResourceResponse> found = resources.search(null, false)
                .stream()
                .limit(8)
                .toList();

        sources.add("CommuneX resource inventory");

        if (found.isEmpty()) {
            return new ChatResponse(
                    "No resource inventory records are currently available "
                            + "in CommuneX. I can't confirm physical stock "
                            + "outside the platform's current records.",
                    sources
            );
        }

        String records = found.stream()
                .map(this::formatResource)
                .collect(Collectors.joining("\n"));

        String answer = "Current recorded inventory:\n"
                + records
                + "\nThese are platform records, not a guarantee "
                + "of current physical availability.";

        return new ChatResponse(answer, sources);
    }

    // Format a single resource record
    private String formatResource(ResourceResponse resource) {

        String location = resource.location();

        if (location == null) {
            location = "not specified";
        }

        return "• " + resource.name()
                + ": " + resource.quantity()
                + " " + resource.unit()
                + " — " + resource.status()
                + " (recorded location: " + location + ")";
    }

    // Call the optional locally configured Ollama model
    private String callOptionalLocalModel(String message) {

        if (aiBaseUrl == null || aiBaseUrl.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> body = Map.of(
                    "model",
                    aiModel,
                    "stream",
                    false,
                    "messages",
                    List.of(
                            Map.of(
                                    "role",
                                    "system",
                                    "content",
                                    "You are CommuneX Assistant. "
                                            + "Do not claim access to unseen "
                                            + "data. Do not provide fabricated "
                                            + "facility, inventory, or route "
                                            + "facts. The user request has "
                                            + "already been screened for "
                                            + "platform tools; answer only "
                                            + "general questions here. Do not "
                                            + "claim to execute actions."
                            ),
                            Map.of(
                                    "role",
                                    "user",
                                    "content",
                                    message
                            )
                    )
            );

            String json = mapper.writeValueAsString(body);

            String baseUrl = aiBaseUrl.replaceAll("/+$", "");

            String response = restClient.post()
                    .uri(baseUrl + "/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);

            JsonNode node = mapper.readTree(response);

            return node.path("message")
                    .path("content")
                    .asText(null);

        } catch (Exception exception) {
            return null;
        }
    }

    // Check whether the message contains any specified keyword
    private boolean containsAny(
            String message,
            String... values
    ) {
        for (String value : values) {
            if (message.contains(value)) {
                return true;
            }
        }

        return false;
    }

    // Chat request DTO
    public record ChatRequest(
            @NotBlank
            @Size(max = 1500)
            String message
    ) {
    }

    // Chat response DTO
    public record ChatResponse(
            String answer,
            List<String> sources
    ) {
    }
}