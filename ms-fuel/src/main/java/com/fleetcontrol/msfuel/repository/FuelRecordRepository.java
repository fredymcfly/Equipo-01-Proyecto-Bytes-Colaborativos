package com.fleetcontrol.msfuel.repository;

import com.fleetcontrol.msfuel.model.FuelRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a los repostajes almacenados. */
@Repository
public interface FuelRecordRepository extends JpaRepository<FuelRecord, UUID> {

  /** Último repostaje del vehículo, sea del tipo que sea. */
  Optional<FuelRecord> findFirstByVehicleIdOrderByRefueledAtDesc(UUID vehicleId);

  /** Último repostaje con el depósito lleno, que es el punto de partida del consumo. */
  Optional<FuelRecord> findFirstByVehicleIdAndFullTankTrueOrderByRefueledAtDesc(UUID vehicleId);

  /** Repostajes del vehículo dentro del intervalo, del más reciente al más antiguo. */
  List<FuelRecord> findByVehicleIdAndRefueledAtBetweenOrderByRefueledAtDesc(
      UUID vehicleId, Instant from, Instant to);

  /** Repostajes de toda la flota dentro del intervalo, del más reciente al más antiguo. */
  List<FuelRecord> findByRefueledAtBetweenOrderByRefueledAtDesc(Instant from, Instant to);

  /**
   * Repostaje con el mayor odómetro del vehículo, contra el que se valida la regla de odómetro.
   *
   * @param vehicleId identificador del vehículo
   * @return el repostaje más avanzado en odómetro, o vacío si aún no tiene repostajes
   */
  Optional<FuelRecord> findFirstByVehicleIdOrderByOdometerKmDesc(UUID vehicleId);

  /**
   * Último repostaje con depósito lleno anterior al odómetro indicado, punto de partida del cálculo
   * de consumo.
   *
   * @param vehicleId identificador del vehículo
   * @param odometerKm odómetro del repostaje que se está registrando
   * @return el lleno anterior, o vacío si todavía no hay ninguno
   */
  Optional<FuelRecord>
      findFirstByVehicleIdAndFullTankTrueAndOdometerKmLessThanOrderByOdometerKmDesc(
          UUID vehicleId, Integer odometerKm);

  /**
   * Repostajes situados estrictamente entre los dos llenos, que son los litros parciales
   * intermedios.
   *
   * @param vehicleId identificador del vehículo
   * @param desde odómetro del lleno anterior, excluido
   * @param hasta odómetro del lleno actual, excluido
   * @return los repostajes intermedios, del más antiguo al más reciente
   */
  List<FuelRecord> findByVehicleIdAndOdometerKmGreaterThanAndOdometerKmLessThanOrderByOdometerKmAsc(
      UUID vehicleId, Integer desde, Integer hasta);
}
