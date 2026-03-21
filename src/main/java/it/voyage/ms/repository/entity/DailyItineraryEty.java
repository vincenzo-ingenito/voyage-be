package it.voyage.ms.repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_itineraries", indexes = {
    @Index(name = "idx_daily_itinerary_travel_id", columnList = "travel_id"),
    @Index(name = "idx_daily_itinerary_day",       columnList = "day")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyItineraryEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "day", nullable = false)
    private Integer day;

    @Column(name = "date", length = 50)
    private String date;

    @Column(name = "memory_image_url", length = 500)
    private String memoryImageUrl;

    @Column(name = "memory_image_index")
    private Integer memoryImageIndex;

    /** Relation: Travel 1:N DailyItinerary */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_daily_itinerary_travel"))
    private TravelEty travel;

    /** Relation: DailyItinerary 1:N Point — ordinati per order_index */
    @OneToMany(mappedBy = "dailyItinerary", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<PointEty> points = new ArrayList<>();
}