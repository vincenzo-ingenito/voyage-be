package it.voyage.ms.repository.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points", indexes = {
    @Index(name = "idx_point_daily_itinerary_id", columnList = "daily_itinerary_id"),
    @Index(name = "idx_point_order",              columnList = "order_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointEty {

    /**
     * Converte List<Integer> in una stringa CSV ("1,3,7") e viceversa.
     * Più semplice di un JSON grezzo e leggibile nel DB.
     * Sostituisce il vecchio campo attachmentIndicesJson (TEXT raw).
     */
    @Converter
    public static class IntegerListConverter
            implements AttributeConverter<List<Integer>, String> {

        @Override
        public String convertToDatabaseColumn(List<Integer> list) {
            if (list == null || list.isEmpty()) return null;
            return list.stream()
                       .map(String::valueOf)
                       .collect(Collectors.joining(","));
        }

        @Override
        public List<Integer> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return new ArrayList<>();
            return Arrays.stream(dbData.split(","))
                         .map(String::trim)
                         .filter(s -> !s.isEmpty())
                         .map(Integer::parseInt)
                         .collect(Collectors.toList());
        }
    }

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

    /**
     * Indici dei file allegati a questo punto.
     * Convertiti automaticamente da/a List<Integer> tramite IntegerListConverter.
     * Colonna rinominata da attachment_indices_json ad attachment_indices
     * ora che non è più un blob JSON grezzo.
     */
//    @Convert(converter = IntegerListConverter.class)
//    @Column(name = "attachment_indices", length = 500)
//    private List<Integer> attachmentIndices = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(
        name = "point_attachment_indices",
        joinColumns = @JoinColumn(name = "point_id"),
        indexes = @Index(name = "idx_point_attachment_point_id", columnList = "point_id")
    )
    @Column(name = "attachment_index", nullable = false)
    @OrderColumn(name = "position")
    private List<Integer> attachmentIndices = new ArrayList<>();

    /** Relation: DailyItinerary 1:N Point */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_itinerary_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_point_daily_itinerary"))
    private DailyItineraryEty dailyItinerary;
}