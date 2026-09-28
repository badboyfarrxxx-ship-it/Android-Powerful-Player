#include <windows.h>
#include <shellapi.h>
#include <iostream>
#include <string>
#include "../service/VpnService.h"

#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "user32.lib")

#define WM_TRAYICON (WM_USER + 1)
#define ID_TRAY_ICON 1
#define ID_MENU_TOGGLE 101
#define ID_MENU_SETTINGS 102
#define ID_MENU_EXIT 103

NOTIFYICONDATA nid = {};
VpnService vpn;

LRESULT CALLBACK WindowProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    if (msg == WM_TRAYICON) {
        if (wParam == WM_LBUTTONUP) {
            // Show simple status message on left click
            MessageBox(hwnd, vpn.IsRunning() ? "VPN is Connected" : "VPN is Disconnected", "Home VPN", MB_OK | MB_ICONINFORMATION);
        } else if (wParam == WM_RBUTTONUP) {
            // Show context menu on right click
            POINT pt;
            GetCursorPos(&pt);
            HMENU hMenu = CreatePopupMenu();
            AppendMenu(hMenu, MF_STRING, ID_MENU_TOGGLE, vpn.IsRunning() ? "Disconnect VPN" : "Connect VPN");
            AppendMenu(hMenu, MF_SEPARATOR, 0, NULL);
            AppendMenu(hMenu, MF_STRING, ID_MENU_SETTINGS, "Onboarding/Settings");
            AppendMenu(hMenu, MF_STRING, ID_MENU_EXIT, "Exit");
            
            SetForegroundWindow(hwnd);
            TrackPopupMenu(hMenu, TPM_LEFTALIGN | TPM_RIGHTBUTTON, pt.x, pt.y, 0, hwnd, NULL);
            DestroyMenu(hMenu);
        }
    } else if (msg == WM_COMMAND) {
        int wmId = LOWORD(wParam);
        if (wmId == ID_MENU_TOGGLE) {
            if (vpn.IsRunning()) vpn.Stop(); else vpn.Start();
        } else if (wmId == ID_MENU_SETTINGS) {
            // Trigger registration flow (simplified for this demo)
            vpn.RegisterDevice("yourname.duckdns.org", "Windows-PC");
        } else if (wmId == ID_MENU_EXIT) {
            PostQuitMessage(0);
        }
    } else if (msg == WM_DESTROY) {
        Shell_NotifyIcon(NIM_DELETE, &nid);
        PostQuitMessage(0);
    }
    return DefWindowProc(hwnd, msg, wParam, lParam);
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    const char* className = "HomeVpnTray";
    WNDCLASS wc = {};
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = hInstance;
    wc.lpszClassName = className;
    RegisterClass(&wc);

    HWND hwnd = CreateWindowEx(0, className, "HomeVpn", 0, 0, 0, 0, 0, HWND_MESSAGE, NULL, hInstance, NULL);

    nid.cbSize = sizeof(NOTIFYICONDATA);
    nid.hWnd = hwnd;
    nid.uID = ID_TRAY_ICON;
    nid.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
    nid.uCallbackMessage = WM_TRAYICON;
    nid.hIcon = LoadIcon(NULL, IDI_SHIELD);
    strcpy_s(nid.szTip, "Home VPN - Connected");

    Shell_NotifyIcon(NIM_ADD, &nid);

    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return (int)msg.wParam;
}
