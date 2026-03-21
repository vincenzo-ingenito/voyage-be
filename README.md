erDiagram
    UserEty["UserEty"] {
        string id PK
        string name
        string email
        boolean isPrivate
        date createdAt
    }

    TravelEty["TravelEty"] {
        long id PK
        string userId FK
        string travelName
        string dateFrom
        string dateTo
    }

    DailyItineraryEty["DailyItineraryEty"] {
        long id PK
        long travelId FK
        int day
        string date
        int memoryImageIndex
    }

    PointEty["PointEty"] {
        long id PK
        long itineraryId FK
        string name
        double lat
        double lng
        string country
        string attachmentIndices
    }

    TravelFileEty["TravelFileEty"] {
        long id PK
        long travelId FK
        string fileId
        string fileName
        string mimeType
    }

    BookmarkEty["BookmarkEty"] {
        long id PK
        string userId FK
        long travelId FK
        string travelOwnerId
    }

    FriendRelationshipEty["FriendRelationshipEty"] {
        long id PK
        string requesterId FK
        string receiverId FK
        string status
    }

    UserEty ||--o{ TravelEty : possiede
    UserEty ||--o{ BookmarkEty : salva
    UserEty ||--o{ FriendRelationshipEty : invia
    UserEty ||--o{ FriendRelationshipEty : riceve
    TravelEty ||--o{ DailyItineraryEty : ha
    TravelEty ||--o{ TravelFileEty : ha
    TravelEty ||--o{ BookmarkEty : in
    DailyItineraryEty ||--o{ PointEty : ha