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

    UserEty ||--o{ TravelEty : owns
    UserEty ||--o{ BookmarkEty : bookmarks
    UserEty ||--o{ FriendRelationshipEty : sends
    UserEty ||--o{ FriendRelationshipEty : receives
    TravelEty ||--o{ DailyItineraryEty : contains
    TravelEty ||--o{ TravelFileEty : has
    TravelEty ||--o{ BookmarkEty : referenced_by
    DailyItineraryEty ||--o{ PointEty : includes