
import { useEffect, useRef, useState } from "react";

const GOOGLE_SCRIPT_ID = "google-identity-services";
const GOOGLE_CALLBACKS_KEY = "__communexGoogleCallbacks";
const GOOGLE_CLIENT_KEY = "__communexGoogleInitializedClientId";

export default function GoogleSignInButton({
  onCredential,
  onError,
  disabled = false,
}) {
  const buttonRef = useRef(null);
  const callbacksRef = useRef({ onCredential, onError });

  const [ready, setReady] = useState(false);

  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim();

  // Keep the latest callbacks without reinitializing Google's global SDK.
  useEffect(() => {
    callbacksRef.current = {
      onCredential,
      onError,
    };
  }, [onCredential, onError]);

  useEffect(() => {
    if (!clientId) {
      return undefined;
    }

    let cancelled = false;
    let scriptElement = null;
    let loadHandler = null;
    let errorHandler = null;

    function initializeGoogle() {
      if (
        cancelled ||
        !window.google?.accounts?.id ||
        !buttonRef.current
      ) {
        return;
      }

      const previouslyInitializedClientId =
        window[GOOGLE_CLIENT_KEY];

      window[GOOGLE_CALLBACKS_KEY] = callbacksRef;

      // Initialize once per client ID, including React StrictMode remounts.
      if (previouslyInitializedClientId !== clientId) {
        window.google.accounts.id.initialize({
          client_id: clientId,

          callback: (response) => {
            const latestCallbacks =
              window[GOOGLE_CALLBACKS_KEY]?.current;

            if (!response?.credential) {
              latestCallbacks?.onError?.(
                "Google did not return a credential. Please try again."
              );
              return;
            }

            latestCallbacks?.onCredential?.(
              response.credential
            );
          },

          auto_select: false,
          cancel_on_tap_outside: true,
        });

        window[GOOGLE_CLIENT_KEY] = clientId;
      }

      buttonRef.current.innerHTML = "";

      window.google.accounts.id.renderButton(
        buttonRef.current,
        {
          type: "standard",
          theme: "outline",
          size: "large",
          text: "continue_with",
          shape: "rectangular",
          logo_alignment: "left",
          width: Math.max(
            240,
            Math.floor(
              buttonRef.current.getBoundingClientRect().width
            )
          ),
        }
      );

      setReady(true);
    }

    function handleScriptError() {
      callbacksRef.current.onError?.(
        "Could not load Google sign-in. Check your internet connection."
      );
    }

    const existingScript =
      document.getElementById(GOOGLE_SCRIPT_ID);

    if (window.google?.accounts?.id) {
      initializeGoogle();
    } else if (existingScript) {
      scriptElement = existingScript;
      loadHandler = initializeGoogle;
      errorHandler = handleScriptError;

      existingScript.addEventListener(
        "load",
        loadHandler,
        { once: true }
      );

      existingScript.addEventListener(
        "error",
        errorHandler,
        { once: true }
      );
    } else {
      scriptElement = document.createElement("script");

      scriptElement.id = GOOGLE_SCRIPT_ID;
      scriptElement.src =
        "https://accounts.google.com/gsi/client";
      scriptElement.async = true;
      scriptElement.defer = true;

      loadHandler = initializeGoogle;
      errorHandler = handleScriptError;

      scriptElement.addEventListener(
        "load",
        loadHandler,
        { once: true }
      );

      scriptElement.addEventListener(
        "error",
        errorHandler,
        { once: true }
      );

      document.head.appendChild(scriptElement);
    }

    return () => {
      cancelled = true;

      if (scriptElement && loadHandler) {
        scriptElement.removeEventListener(
          "load",
          loadHandler
        );
      }

      if (scriptElement && errorHandler) {
        scriptElement.removeEventListener(
          "error",
          errorHandler
        );
      }
    };
  }, [clientId]);

  if (!clientId) {
    return (
      <div>
        <button
          className="google-signin-button"
          type="button"
          disabled={disabled}
          onClick={() =>
            callbacksRef.current.onError?.(
              "Google sign-in setup required: configure VITE_GOOGLE_CLIENT_ID in frontend/.env.local."
            )
          }
        >
          <span className="google-g" aria-hidden="true">
            G
          </span>

          Continue with Google
        </button>

        <p className="google-help">
          Google sign-in needs a configured OAuth Client ID.
        </p>
      </div>
    );
  }

  return (
    <div
      className={`google-button-shell${
        disabled ? " is-disabled" : ""
      }`}
    >
      <div
        ref={buttonRef}
        className="google-button-host"
      />

      {disabled && (
        <div
          className="google-button-disabled-cover"
          aria-hidden="true"
        />
      )}

      {!ready && (
        <span className="google-loading-label">
          Loading Google sign-in…
        </span>
      )}
    </div>
  );
}
