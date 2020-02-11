package wind_solar

import squants.energy._
import squants.time.Hours
import squants.radio.WattsPerSquareMeter
import squants.space._
import squants.mass.Mass

trait RenewableTechnology {
  val name: String;
  val lifeTime: Int;
  val wind: Boolean; val solar: Boolean; val csp: Boolean; val pv: Boolean;

  val occupationRatio: Double

  val ee: EmbodiedEnergy

  // Operational electricity should be directly removed from potential as it never gets out of the facility ..
  def potential(cell: Cell, eroi_min: Double): Power;

  val excludedCountries = List("NA", "Antarctica", "Greenland", "French Southern & Antarctic Lands")
  def suitabilityFactor(cell: Cell): Double = {
    if (excludedCountries.contains(cell.country) || cell.country.contains("Is.") || cell.country.contains("Islands")) 0.0
    else (1 - cell.protectedArea)
  }
  def ratedPower(cell: Cell, eroi_min: Double): Power;

  def capacityFactor(cell: Cell, eroi_min: Double) = potential(cell, eroi_min: Double) / ratedPower(cell, eroi_min)
  def netYearlyProduction(cell: Cell, eroi_min: Double): Energy =
    if (eroi(cell, eroi_min) >= eroi_min) potential(cell, eroi_min) * Hours(365 * 24) - embodiedEnergy(cell, eroi_min) / lifeTime
    else Joules(0)

  // In EROI calculation we take into account operation variable in the numerator and denominator !
  def eroi(cell: Cell, eroi_min: Double): Double = {
    val wi = ratedPower(cell, eroi_min)
    if (wi.value == 0) 0.0
    else {
      val out_year = potential(cell, eroi_min) * Hours(365 * 24)
      out_year / (1 - operation_variable) * lifeTime / (embodiedEnergy(cell, eroi_min) + out_year / (1 - operation_variable) * operation_variable * lifeTime)
    }
  }

  // Methods that needs to be specifically implemented within each renewable technology class
  // "Upfront" energy inputs: raw materials extractin & processing, transport to site, manufaturing & installation
  def energyInputsInstallation(cell: Cell, eroi_min: Double): Energy = ee.energyInputsInstallation(ratedPower(cell, eroi_min))
  def energyInputsInstallation(cell: Cell): Energy = energyInputsInstallation(cell, 1.0)

  def energyInputsDecomissioning(cell: Cell, eroi_min: Double): Energy = ee.energyInputsDecomissioning(ratedPower(cell, eroi_min))
  def energyInputsDecomissioning(cell: Cell): Energy = energyInputsDecomissioning(cell, 1.0)

  def energyInputsOMYearly(cell: Cell, eroi_min: Double): Energy = ee.energyInputsOMYearly(ratedPower(cell, eroi_min))
  def energyInputsOMYearly(cell: Cell): Energy = energyInputsOMYearly(cell, 1.0)

  def operation_variable: Double = ee.om_output
  
  def embodiedEnergy(cell: Cell, eroi_min: Double): Energy = energyInputsInstallation(cell, eroi_min) + energyInputsDecomissioning(cell, eroi_min) + energyInputsOMYearly(cell, eroi_min) * lifeTime
  def embodiedEnergy(cell: Cell): Energy = embodiedEnergy(cell, 1.0)

  // Ideally should not be used as it overlooks some specific energy inputs linked to the technology itself 
  def embodiedEnergy(rated_power: Power): Energy = ee.embodiedEnergy(rated_power)

}

/**
 * For a given installed capacity (i.e. power installed), the fixed energy inputs for:
 *  - raw materials extraction & processing
 *  - manufacturing
 *  - installation of the facility
 *  - decommissioning
 *  - transport at every stage
 *  - annual O&M energy inputs
 *  - variable operational energy inputs (% of output)
 *
 *  Specific dependance (on water depth for offshore wind foundations, or aperture area for CSP power plant)
 *  should be implemented in corresponding renewable technology classes
 */
class EmbodiedEnergy(val power: Power,
    val raw_materials: Energy, val manufacturing: Energy,
    val installation: Energy, val decommissioning: Energy,
    val transport: Energy, val om_fixed: Energy,
    val om_output: Double, val lifeTime: Int) {
  
  def ratioPower(rated_power: Power): Double = rated_power/power
  
  def embodiedEnergy(rated_power: Power): Energy = energyInputsInstallation(rated_power) + energyInputsDecomissioning(rated_power) + lifeTime*energyInputsOMYearly(rated_power)
  //def embodiedEnergy(rated_power: Power, output: Energy): Energy = embodiedEnergy(rated_power) + output * om_output
  
  def energyInputsInstallation(rated_power: Power): Energy = ratioPower(rated_power) * (raw_materials + manufacturing + installation + transport)
  def energyInputsDecomissioning(rated_power: Power): Energy = ratioPower(rated_power) * decommissioning
  def energyInputsOMYearly(rated_power: Power): Energy = ratioPower(rated_power) * om_fixed

  def truckTransport(weight: Mass, distance: Length) = Megajoules(1.25) * weight.toTonnes * Math.abs(distance.toKilometers)
  def shipTransport(weight: Mass, distance: Length) = Megajoules(0.29) * weight.toTonnes * Math.abs(distance.toKilometers)

  // For CSP, the embodied energy was calculated for a default aperture area ! 
  /*  def embodiedEnergyArea(rated_power: Power, area: Area): Energy = {
    val area_ratio = area / default_area
    embodiedEnergy(rated_power) + area_ratio * (transport_variable + construction_variable)
  }

  // For Wind energy, transport varies with distance to shore
  def embodiedEnergyDistance(rated_power: Power, distanceToShore: Length): Energy = {
    distanceToShore.toKilometers * (construction_variable + om_variable)
  }

  def installationEnergyInputs(rated_power: Power, area: Area): Energy = {
    val area_ratio = area / default_area
    val ratio = rated_power / power
    (raw_materials + manufacturing + installation + transport) * ratio + area_ratio * (transport_variable + construction_variable)
  }*/
  // def omYearlyEnergyInputs(rated_power: Power): Energy = rated_power / power * om_fixed
}
