package com.fleetcontrol.msfuel.repository;

import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.repository.projection.FuelPeriodAggregate;
import com.fleetcontrol.msfuel.repository.projection.FuelTotalsAggregate;
import com.fleetcontrol.msfuel.repository.projection.VehicleFuelAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a los repostajes almacenados. */
@Repository
public interface FuelRecordRepository extends JpaRepository<FuelRecord, UUID> {

  String FROM_PARAM = "from";
  String TO_PARAM = "to";

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

  // --- Listado paginado ---

  /** Repostajes del vehículo, paginados según el orden recibido. */
  Page<FuelRecord> findByVehicleId(UUID vehicleId, Pageable pageable);

  /** Repostajes de toda la flota dentro del intervalo, paginados. */
  Page<FuelRecord> findByRefueledAtBetween(Instant from, Instant to, Pageable pageable);

  /** Repostajes del vehículo dentro del intervalo, paginados. */
  Page<FuelRecord> findByVehicleIdAndRefueledAtBetween(
      UUID vehicleId, Instant from, Instant to, Pageable pageable);

  // --- Agregados (SQL nativo) ---

  /**
   * Litros, coste, consumo medio y odómetros de cada vehículo dentro del intervalo.
   *
   * @param from inicio del intervalo, incluido
   * @param to fin del intervalo, excluido
   * @return una fila por vehículo, ordenada por coste descendente
   */
  default List<VehicleFuelAggregate> aggregateByVehicle(Instant from, Instant to) {
    return selectVehicleAggregates(from, to).stream().map(VehicleFuelAggregate::fromRow).toList();
  }

  @Query(
      value =
          """
          SELECT vehicle_id, SUM(liters), SUM(total_cost), AVG(consumption_l100km),
                 MIN(odometer_km), MAX(odometer_km)
          FROM fuel_records
          WHERE refueled_at >= :from AND refueled_at < :to
          GROUP BY vehicle_id
          ORDER BY SUM(total_cost) DESC
          """,
      nativeQuery = true)
  List<Object[]> selectVehicleAggregates(
      @Param(FROM_PARAM) Instant from, @Param(TO_PARAM) Instant to);

  /**
   * Serie temporal de la flota agrupada por periodo.
   *
   * @param from inicio del intervalo, incluido
   * @param to fin del intervalo, excluido
   * @param granularity una de {@code day}, {@code week} o {@code month}
   * @return una fila por periodo con datos, del más antiguo al más reciente
   */
  default List<FuelPeriodAggregate> aggregateByPeriod(
      Instant from, Instant to, String granularity) {
    return selectPeriodAggregates(from, to, granularity).stream()
        .map(FuelPeriodAggregate::fromRow)
        .toList();
  }

  @Query(
      value =
          """
          SELECT to_char(date_trunc(CAST(:granularity AS text), refueled_at AT TIME ZONE 'UTC'),
                         'YYYY-MM-DD') AS period,
                 SUM(liters), SUM(total_cost), AVG(price_per_liter), AVG(consumption_l100km)
          FROM fuel_records
          WHERE refueled_at >= :from AND refueled_at < :to
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> selectPeriodAggregates(
      @Param(FROM_PARAM) Instant from,
      @Param(TO_PARAM) Instant to,
      @Param("granularity") String granularity);

  /**
   * Serie temporal de un vehículo agrupada por periodo.
   *
   * @param from inicio del intervalo, incluido
   * @param to fin del intervalo, excluido
   * @param granularity una de {@code day}, {@code week} o {@code month}
   * @param vehicleId vehículo del que se calcula la serie
   * @return una fila por periodo con datos, del más antiguo al más reciente
   */
  default List<FuelPeriodAggregate> aggregateByPeriodAndVehicle(
      Instant from, Instant to, String granularity, UUID vehicleId) {
    return selectPeriodAggregatesByVehicle(from, to, granularity, vehicleId).stream()
        .map(FuelPeriodAggregate::fromRow)
        .toList();
  }

  @Query(
      value =
          """
          SELECT to_char(date_trunc(CAST(:granularity AS text), refueled_at AT TIME ZONE 'UTC'),
                         'YYYY-MM-DD') AS period,
                 SUM(liters), SUM(total_cost), AVG(price_per_liter), AVG(consumption_l100km)
          FROM fuel_records
          WHERE refueled_at >= :from AND refueled_at < :to AND vehicle_id = :vehicleId
          GROUP BY period
          ORDER BY period
          """,
      nativeQuery = true)
  List<Object[]> selectPeriodAggregatesByVehicle(
      @Param(FROM_PARAM) Instant from,
      @Param(TO_PARAM) Instant to,
      @Param("granularity") String granularity,
      @Param("vehicleId") UUID vehicleId);

  /**
   * Totales de la flota dentro del intervalo.
   *
   * @param from inicio del intervalo, incluido
   * @param to fin del intervalo, excluido
   * @return litros, coste, precio medio y consumo medio del intervalo
   */
  default FuelTotalsAggregate totals(Instant from, Instant to) {
    return FuelTotalsAggregate.fromRow(selectTotals(from, to).get(0));
  }

  @Query(
      value =
          """
          SELECT SUM(liters), SUM(total_cost), AVG(price_per_liter), AVG(consumption_l100km)
          FROM fuel_records
          WHERE refueled_at >= :from AND refueled_at < :to
          """,
      nativeQuery = true)
  List<Object[]> selectTotals(@Param(FROM_PARAM) Instant from, @Param(TO_PARAM) Instant to);

  /**
   * Totales de un vehículo dentro del intervalo.
   *
   * @param from inicio del intervalo, incluido
   * @param to fin del intervalo, excluido
   * @param vehicleId vehículo del que se calculan los totales
   * @return litros, coste, precio medio y consumo medio del intervalo
   */
  default FuelTotalsAggregate totalsByVehicle(Instant from, Instant to, UUID vehicleId) {
    return FuelTotalsAggregate.fromRow(selectTotalsByVehicle(from, to, vehicleId).get(0));
  }

  @Query(
      value =
          """
          SELECT SUM(liters), SUM(total_cost), AVG(price_per_liter), AVG(consumption_l100km)
          FROM fuel_records
          WHERE refueled_at >= :from AND refueled_at < :to AND vehicle_id = :vehicleId
          """,
      nativeQuery = true)
  List<Object[]> selectTotalsByVehicle(
      @Param(FROM_PARAM) Instant from,
      @Param(TO_PARAM) Instant to,
      @Param("vehicleId") UUID vehicleId);
}
