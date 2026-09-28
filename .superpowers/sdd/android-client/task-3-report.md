# Task 3 Report: UI and Connection Management

## UI Architecture

The Android client implements a modern unidirectional data flow (UDF) architecture using Jetpack Compose and `StateFlow`.

### State Management
- **VpnViewModel**: Acts as the single source of truth for the UI state. It exposes a `VpnUiState` data class containing the connection status (`DISCONNECTED`, `CONNECTING`, `CONNECTED`), assigned VPN IP, and real-time throughput metrics.
- **StateFlow**: The UI observes the `uiState` Flow, ensuring that the interface updates automatically whenever the underlying state changes.
- **Metrics Polling**: A background polling mechanism is implemented in the ViewModel to periodically fetch bytes sent/received from the `HomeVpnService` to calculate KB/s throughput.

### Screen Implementations
1. **VpnScreen**:
   - **Connection Toggle**: A prominent button that triggers the VPN start/stop sequence.
   - **Metrics Dashboard**: A card-based layout displaying real-time status, the assigned internal VPN IP, and upload/download speeds.
2. **OnboardingScreen**:
   - A guided setup flow to collect the `Server Domain` and `Device Name`.
   - Integration with the Raspberry Pi gateway's `/register` endpoint via `VpnNetworkClient`.

## Onboarding Sequence

The onboarding process ensures the device is properly registered and configured before the VPN can be established.

1. **Data Collection**: The user enters the server domain (e.g., `home.duckdns.org`) and a unique device name.
2. **Registration**: 
   - The client sends a POST request to `https://<server_domain>/register` with the device name.
   - The server validates the request and returns the **Server Public Key** and the **Assigned VPN IP**.
3. **Secure Storage**:
   - The returned configuration is stored using `EncryptedSharedPreferences` via the `VpnPreferences` helper class.
   - This ensures that the public key and assigned IP are encrypted at rest.
4. **Transition**: Upon successful registration, the app transitions the user to the main `VpnScreen`.

## Service Integration

- **Permission Handling**: The ViewModel uses `VpnService.prepare(context)` to check for the `VpnService` permission. If not granted, it launches the system's permission request dialog.
- **Lifecycle Management**: The VPN is controlled via `Intent` calls to `HomeVpnService`, allowing the service to run in the foreground with a persistent notification, preventing the OS from killing the connection.
