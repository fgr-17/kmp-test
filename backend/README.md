# Cloud backend (Go)

Tiny in-memory API that plays the **system of record** for the offline notes demo.

## Run

```bash
cd backend
go run .
```

Listens on `http://localhost:8081`.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/health` | Liveness |
| `GET` | `/notes` | All notes stored in the cloud |
| `POST` | `/notes/batch` | Accept pending notes from a device |

### Batch body

```json
{
  "notes": [
    { "id": "…", "text": "Checked barrel B12", "createdAt": "2026-10-17T04:00:00Z" }
  ]
}
```

### Batch response

```json
{ "acceptedIds": ["…"] }
```

Data resets when the process exits (in-memory only — fine for the playground).
