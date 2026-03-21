@startuml
!theme plain
hide circle
skinparam linetype ortho

entity "UserEty" as User {
  +id : String <<PK>>
  --
  name : String
  email : String
  isPrivate : boolean
  createdAt : Date
}

entity "TravelEty" as Travel {
  +id : Long <<PK>>
  --
  user_id : String <<FK>>
  travelName : String
  dateFrom : String
  dateTo : String
}

entity "DailyItineraryEty" as DailyItinerary {
  +id : Long <<PK>>
  --
  travel_id : Long <<FK>>
  day : Integer
  date : String
  memoryImageIndex : Integer
}

entity "PointEty" as Point {
  +id : Long <<PK>>
  --
  daily_itinerary_id : Long <<FK>>
  name : String
  latitude : Double
  longitude : Double
  country : String
  attachmentIndices : String
}

entity "TravelFileEty" as TravelFile {
  +id : Long <<PK>>
  --
  travel_id : Long <<FK>>
  fileId : String
  fileName : String
  mimeType : String
}

entity "BookmarkEty" as Bookmark {
  +id : Long <<PK>>
  --
  user_id : String <<FK>>
  travel_id : Long <<FK>>
  travelOwnerId : String
}

entity "FriendRelationshipEty" as Friendship {
  +id : Long <<PK>>
  --
  requester_id : String <<FK>>
  receiver_id : String <<FK>>
  status : String
}

' ================= RELAZIONI =================

User ||--o{ Travel : possiede
User ||--o{ Bookmark : salva
User ||--o{ Friendship : invia
User ||--o{ Friendship : riceve

Travel ||--o{ DailyItinerary : ha
Travel ||--o{ TravelFile : ha
Travel ||--o{ Bookmark : in

DailyItinerary ||--o{ Point : ha

@enduml