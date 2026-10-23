package com.communex.map;

import java.util.List;

/** The server validates the bounding box and allowlists query categories. */
public record MapOverpassRequest(
        String type,
        BoundingBox bbox,
        List<String> categories
) {

    public record BoundingBox(
            double south,
            double west,
            double north,
            double east
    ) {
    }
}
