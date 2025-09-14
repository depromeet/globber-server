package backend.globber.membertravel.service;

import backend.globber.membertravel.controller.dto.CityCoordinates;

public interface GeocodingService {

  CityCoordinates getCoordinates(String cityName, String countryName);
}