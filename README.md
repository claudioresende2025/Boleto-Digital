# Boleto Digital

App Android para digitalizar boletos em papel com vencimento no dia 10. Guarde o **PIX Copia e Cola**, acompanhe o desconto de antecipação (R$ 160 → R$ 150) e pague no app do banco.

## Funcionalidades

- Leitura do boleto por câmera (QR PIX)
- Geração automática dos meses seguintes até a data escolhida
- Cadastro de boletos digitais com valor, desconto e vencimento
- Cópia rápida do código PIX para colar no app do banco
- Alerta de desconto válido **antes do dia 10**
- Marcação de pagamento e histórico local (Room/SQLite)
- APK universal com arquiteturas **32 bits (`armeabi-v7a`)** e **64 bits (`arm64-v8a`)**

## Requisitos

- JDK 17+
- Android SDK (API 35)
- Gradle Wrapper incluso

## Build do APK

```bash
# Debug
./gradlew assembleDebug

# Release (assinado com keystore de debug do Android se não houver release keystore)
./gradlew assembleRelease
```

APKs gerados em:

- `app/build/outputs/apk/debug/`
- `app/build/outputs/apk/release/`

## Uso

1. Instale o APK no celular
2. Toque em **+** e cadastre a mensalidade (padrão: vencimento dia 10, R$ 160, desconto R$ 10)
3. Cole o **PIX Copia e Cola** do boleto em papel
4. No vencimento (ou antes), abra o boleto → **Copiar PIX e pagar no banco**
5. Cole no app do seu banco e conclua o pagamento

## Pacote

- `applicationId`: `com.boletodigital.app`
- `minSdk`: 26
- `targetSdk`: 35
