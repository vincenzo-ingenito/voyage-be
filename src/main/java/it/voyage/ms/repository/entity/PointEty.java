package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points", indexes = {
    @Index(name = "idx_point_daily_itinerary_id", columnList = "daily_itinerary_id"),
    @Index(name = "idx_point_order", columnList = "order_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "photo_reference", length = 500)
    private String photoReference;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cost", length = 50)
    private String cost;

    @Column(name = "country", length = 255)
    private String country;

    @Column(name = "region", length = 255)
    private String region;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "attachment_indices_json", columnDefinition = "TEXT")
    private String attachmentIndicesJson;

    // Relation: DailyItinerary 1:N Point (un punto appartiene a un giorno)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_itinerary_id", nullable = false, foreignKey = @ForeignKey(name = "fk_point_daily_itinerary"))
    private DailyItineraryEty dailyItinerary;
}