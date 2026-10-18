# VxAI — Android Chatbot (Kotlin) for 9Router

Chat app Android (Kotlin) yang connect ke **9Router** (OpenAI-compatible) di `http://host:20128/v1`. Support upload foto (vision) + pilih model + settings.

## Fitur
- Chat UI (RecyclerView bubble)
- Upload foto dari galeri / kamera (base64 image_url, vision models)
- Settings: Base URL, API Key, Model (auto-fetch dari `/v1/models`)
- Cleartext HTTP diizinkan (untuk localhost/LAN 9Router)

## Build
Buka di Android Studio → Run, atau push ke GitHub lalu build via Codemagic (config `codemagic.yaml` sudah ada).

### Emulator
Default base URL `http://10.0.2.2:20128/v1/` (10.0.2.2 = host machine dari emulator).

### Device fisik
Ganti base URL ke IP LAN mesin yang jalanin 9Router, mis. `http://192.168.1.10:20128/v1/`.

## Catatan
- API Key diambil dari dashboard 9Router.
- Permission INTERNET + CAMERA.
- Foto dikompres ke JPEG max 1024px, quality 80 sebelum dikirim (hemat token).
