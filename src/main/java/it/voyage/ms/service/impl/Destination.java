package it.voyage.ms.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Destination {
    String cityName;
    String country;
    double lat;
    double lng;
    List<String> places;
}