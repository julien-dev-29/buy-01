# API — Buy-01

## Base URL

```
http://localhost:9000
```

Toutes les requêtes passent par l'**API Gateway** (port 9000).

---

## Authentification

### Inscription

```
POST /api/auth/register
```

**Body :**
```json
{
  "email": "john@example.com",
  "password": "secret123",
  "firstName": "John",
  "lastName": "Doe",
  "role": "SELLER"
}
```

**Rôles disponibles :** `CLIENT`, `SELLER` (défaut : `CLIENT`).

**Réponse (201) :**
```json
{
  "id": "664f1a...",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "SELLER",
  "avatar": null,
  "createdAt": "2025-07-29T10:00:00",
  "updatedAt": "2025-07-29T10:00:00"
}
```

---

### Connexion

```
POST /api/auth/login
```

**Body :**
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

**Réponse (200) :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Ce token JWT est à inclure dans les requêtes authentifiées via l'en-tête `Authorization: Bearer <token>`.

---

## Profil utilisateur

Toutes les routes `/api/users/*` nécessitent un token JWT valide.

### Récupérer son profil

```
GET /api/users/me
Authorization: Bearer <token>
```

**Réponse (200) :**
```json
{
  "id": "664f1a...",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "SELLER",
  "avatar": null,
  "createdAt": "2025-07-29T10:00:00",
  "updatedAt": "2025-07-29T10:00:00"
}
```

### Modifier son profil

```
PUT /api/users/me
Authorization: Bearer <token>
```

**Body (champs optionnels) :**
```json
{
  "firstName": "Johnny",
  "avatar": "https://example.com/avatar.jpg"
}
```

**Réponse (200) :** `UserDTO` mis à jour.

---

## Produits

### Liste des produits (public)

```
GET /api/products
```

**Réponse (200) :**
```json
[
  {
    "id": "664f1b...",
    "name": "MacBook Pro",
    "description": "16 pouces, M3 Pro",
    "price": 2499.99,
    "sellerId": "664f1a...",
    "mediaIds": ["664f1c..."],
    "createdAt": "2025-07-29T11:00:00",
    "updatedAt": "2025-07-29T11:00:00"
  }
]
```

### Détail d'un produit (public)

```
GET /api/products/{id}
```

### Créer un produit (authentifié)

```
POST /api/products
Authorization: Bearer <token>
```

**Body :**
```json
{
  "name": "MacBook Pro",
  "description": "16 pouces, M3 Pro",
  "price": 2499.99,
  "mediaIds": ["664f1c..."]
}
```

**Réponse (201) :** `ProductDTO`

### Modifier un produit (authentifié, propriétaire)

```
PUT /api/products/{id}
Authorization: Bearer <token>
```

**Body (champs optionnels) :**
```json
{
  "price": 2299.99
}
```

**Réponse (200) :** `ProductDTO` mis à jour.

### Supprimer un produit (authentifié, propriétaire)

```
DELETE /api/products/{id}
Authorization: Bearer <token>
```

**Réponse (204) :** Pas de contenu.

---

## Médias

### Uploader un fichier (authentifié)

```
POST /api/media/upload?productId=664f1b...
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

| Champ   | Type        | Requis |
|---------|-------------|--------|
| `file`  | `image/*`   | Oui    |
| `productId` | string | Non    |

**Formats acceptés :** `image/jpeg`, `image/png`, `image/gif`, `image/webp`
**Taille max :** 2 Mo

**Réponse (201) :**
```json
{
  "id": "664f1c...",
  "filename": "abc123.jpg",
  "originalName": "photo.jpg",
  "contentType": "image/jpeg",
  "size": 123456,
  "productId": "664f1b...",
  "sellerId": "664f1a...",
  "createdAt": "2025-07-29T12:00:00"
}
```

**Erreur (400) :**
```json
{
  "error": "INVALID_FILE_TYPE",
  "message": "Only JPEG, PNG, GIF and WebP images are allowed",
  "timestamp": "2025-07-29T12:00:00"
}
```

### Récupérer un média (public)

```
GET /api/media/{id}
```

**Réponse (200) :** `MediaDTO`

### Médias d'un produit (public)

```
GET /api/media/product/{productId}
```

**Réponse (200) :** `MediaDTO[]`

### Supprimer un média (authentifié, propriétaire)

```
DELETE /api/media/{id}
Authorization: Bearer <token>
```

**Réponse (204) :** Pas de contenu.

---

## Erreurs

Les erreurs suivent un format uniforme :

```json
{
  "error": "INVALID_FILE_TYPE",
  "message": "Description de l'erreur",
  "timestamp": "2025-07-29T12:00:00"
}
```

| Code | Signification |
|------|---------------|
| 400  | Requête invalide (validation, fichier trop volumineux, etc.) |
| 401  | Token JWT manquant, invalide ou expiré |
| 403  | Accès refusé (pas propriétaire de la ressource) |
| 404  | Ressource introuvable |
| 409  | Conflit (email déjà utilisé) |
| 500  | Erreur interne |

---

## Flow complet (exemple)

```bash
# 1. Inscription
curl -s -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123","role":"SELLER"}'

# 2. Connexion
TOKEN=$(curl -s -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123"}' \
  | jq -r '.token')

# 3. Créer un produit
curl -s -X POST http://localhost:9000/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"MacBook Pro","price":2499.99}'

# 4. Lister les produits (public)
curl -s http://localhost:9000/api/products
```
