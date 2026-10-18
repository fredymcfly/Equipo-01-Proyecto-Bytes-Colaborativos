package com.fleetcontrol.msvehicles.constants;

import com.fleetcontrol.msvehicles.models.enums.FuelType;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the sizes, distributions and catalogue used to generate demo data. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoConstants {

  public static final int DEFAULT_SEED = 42;
  public static final int DEFAULT_DAYS = 90;

  public static final String PLATE_LETTERS = "BCDFGHJKLMNPRSTVWXYZ";
  public static final String PLATE_FORMAT = "%04d-%s";
  public static final int PLATE_DIGITS_BOUND = 10_000;
  public static final int PLATE_LETTER_COUNT = 3;

  public static final int MAX_AGE_YEARS = 8;
  public static final int MIN_ODOMETER = 15_000;
  public static final int ODOMETER_RANGE = 70_000;

  /** How many vehicles of a type are generated. */
  public record TypeCount(VehicleType type, int count) {}

  /** How many vehicles end up in a status. */
  public record StatusCount(VehicleStatus status, int count) {}

  /** Make, model and technical data of a catalogue vehicle. */
  public record ModelSpec(String make, String model, FuelType fuelType, int tankCapacityL) {}

  /** Vehicles by type, in generation order: 25 in total. */
  public static final List<TypeCount> TYPE_COUNTS =
      List.of(
          new TypeCount(VehicleType.CAR, 9),
          new TypeCount(VehicleType.VAN, 10),
          new TypeCount(VehicleType.TRUCK, 4),
          new TypeCount(VehicleType.MOTORCYCLE, 2));

  /** Vehicles by status: 25 in total, so that every status is represented. */
  public static final List<StatusCount> STATUS_COUNTS =
      List.of(
          new StatusCount(VehicleStatus.AVAILABLE, 14),
          new StatusCount(VehicleStatus.IN_USE, 8),
          new StatusCount(VehicleStatus.IN_MAINTENANCE, 2),
          new StatusCount(VehicleStatus.OUT_OF_SERVICE, 1));

  /** Models each vehicle type can take. */
  public static final Map<VehicleType, List<ModelSpec>> CATALOGUE =
      Map.of(
          VehicleType.CAR,
          List.of(
              new ModelSpec("Renault", "Clio", FuelType.GASOLINE, 45),
              new ModelSpec("Seat", "Leon", FuelType.GASOLINE, 50),
              new ModelSpec("Toyota", "Corolla", FuelType.HYBRID, 43),
              new ModelSpec("Peugeot", "308", FuelType.DIESEL, 53)),
          VehicleType.VAN,
          List.of(
              new ModelSpec("Ford", "Transit", FuelType.DIESEL, 80),
              new ModelSpec("Mercedes-Benz", "Sprinter", FuelType.DIESEL, 75),
              new ModelSpec("Renault", "Master", FuelType.DIESEL, 80),
              new ModelSpec("Volkswagen", "Crafter", FuelType.DIESEL, 75)),
          VehicleType.TRUCK,
          List.of(
              new ModelSpec("Mercedes-Benz", "Actros", FuelType.DIESEL, 400),
              new ModelSpec("Volvo", "FH", FuelType.DIESEL, 450),
              new ModelSpec("Scania", "R450", FuelType.DIESEL, 400),
              new ModelSpec("Iveco", "S-Way", FuelType.DIESEL, 400)),
          VehicleType.MOTORCYCLE,
          List.of(
              new ModelSpec("Honda", "PCX", FuelType.GASOLINE, 8),
              new ModelSpec("Yamaha", "MT-07", FuelType.GASOLINE, 14),
              new ModelSpec("BMW", "R1250GS", FuelType.GASOLINE, 20)));

  /**
   * Id of the demo vehicle with the given number, from 1. It is derived from the number, so the
   * services that refer to the demo vehicles (maintenance, routes, fuel) compute the same ids
   * without calling this one.
   */
  public static UUID vehicleId(int number) {
    return UUID.nameUUIDFromBytes(("vehicle-" + number).getBytes(StandardCharsets.UTF_8));
  }
}
