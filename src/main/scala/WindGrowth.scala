
import gridData._
import squants.space._
import scala.collection.mutable.ListBuffer
import squants.motion._
import squants.energy._
import utils.Terawatts
import utils.Helper
import utils.TerawattHours
import utils.PlotHelper
import squants.time.Hours
import utils.Exajoules
import windEnergy.CapacityFactorCalculation

object WindGrowth {

  def simulate(annualGrowth: Power, cellIterator: Iterator[GridCell]) {
    var currentYear = 2015
    var newProduction = List[Double]()
    while (currentYear < 2030) {
      currentYear += 1
      var addedPower = Watts(0)
      var addedProduction = WattHours(0)
      while (cellIterator.hasNext && Math.abs(addedPower.toGigawatts - annualGrowth.toGigawatts) > 0.01) {
        var next = cellIterator.next
        val pow = List(WindPotential.powerInstalled(next), annualGrowth - addedPower).min
        addedPower = addedPower + pow
        addedProduction = addedProduction + pow * CapacityFactorCalculation(next) * Hours(365 * 24)
      }
      newProduction = newProduction :+ addedProduction.to(Exajoules)

      println(currentYear + "\t" + addedPower.toGigawatts + "\t" + addedProduction.to(Exajoules))
    }
    val res = newProduction.toList
    PlotHelper.plotXY((2016 to 2030).map(_.toDouble).toList, newProduction.toList)

  }
  def simulateConstantGrowth = {
    val onshoreAnnualGrowth = Gigawatts(254) / 15
    val offshoreAnnualGrowth = Gigawatts(66) / 15

    val europe = new WorldGrid("results/europeGridWind.txt", Degrees(0.25))

    val suitableOnshoreGrids = europe.onshoreGrids.filter(WindPotential.suitabilityFactor(_) > 0).sortBy(g => -CapacityFactorCalculation(g))
    val suitableOffshoreGrids = europe.offshoreGrids.filter(WindPotential.suitabilityFactor(_) > 0).sortBy(g => -CapacityFactorCalculation(g))

    val onshoreCellIterator: Iterator[GridCell] = suitableOnshoreGrids.toIterator
    val onshoreCellsWithTurbine: ListBuffer[(GridCell, Double)] = ListBuffer()
    val offshoreCellIterator: Iterator[GridCell] = suitableOffshoreGrids.toIterator
    val offshoreCellsWithTurbine: ListBuffer[(GridCell, Double)] = ListBuffer()

    simulate(onshoreAnnualGrowth, onshoreCellIterator)
    //  simulate(offshoreAnnualGrowth, offshoreCellIterator)
  }
  def simulate(annualGrowth: List[(Int, Power)], cellIterator: Iterator[GridCell]) {
    var newProduction = List[Double]()
    annualGrowth.map(g => {
      val currentYear = g._1
      val currentGrowth = g._2
      var addedPower = Watts(0)
      var addedProduction = WattHours(0)
      while (cellIterator.hasNext && Math.abs(addedPower.toMegawatts - currentGrowth.toMegawatts) > 0.01) {
        var next = cellIterator.next
        val pow = List(WindPotential.powerInstalled(next), currentGrowth - addedPower).min
        addedPower = addedPower + pow
        addedProduction = addedProduction + pow * CapacityFactorCalculation(next) * Hours(365 * 24)
      }
      newProduction = newProduction :+ addedProduction.to(Exajoules)

      println(currentYear + "\t" + addedPower.toGigawatts + "\t" + addedProduction.to(Exajoules))
    })
    val res = newProduction.toList
    if (annualGrowth.size > 1) PlotHelper.plotXY(annualGrowth.map(_._1.toDouble).toList, newProduction.toList)

  }
  def main(args: Array[String]): Unit = {
    val world = new WorldGrid("results/worldGridWind.txt", Degrees(0.5))
    val growth = Helper.getLines("windGrowth.txt").map(l => (l(0).toInt, Megawatts(l(2).toInt)))
    val cellIterator: Iterator[GridCell] = world.onshoreConstrainedGrids(WindPotential).sortBy(g => -CapacityFactorCalculation(g)).toIterator

    simulate(List((2015, Megawatts(432883))), cellIterator)

  }

  def maxProduction {
    val world = new WorldGrid("results/worldGridWind.txt", Degrees(0.5))

    val F_acc = Newtons(1.1918E14) / world.totalArea
    val z_0 = Millimeters(1)
    val C_DN = 0.0013
    val v_hub_0 = MetersPerSecond(world.grids.map(g => g.windSpeedHub.toMetersPerSecond * g.area.toSquareKilometers).sum / world.totalArea.toSquareKilometers)

    def C_ex = 0.0013

    def v_hub = F_acc
  }

}

object IEWA2030 {
  def main(args: Array[String]): Unit = {

    val wind = new WorldGrid("europe.txt", Degrees(0.25))
    val target = Helper.getLines("iewa_2030.txt").map(l => (Country(l(0)), Megawatts(l(1).toDouble)))

    for (i <- target) {
      val gr = wind.grids.filter(_.country.equals(i._1)).filter(WindPotential.suitabilityFactor(_) > 0).sortBy(g => -CapacityFactorCalculation(g))
      val cellIterator = gr.toIterator
      var addedPower = Watts(0)
      var addedProduction = WattHours(0)
      while (cellIterator.hasNext && Math.abs(addedPower.toMegawatts - i._2.toMegawatts) > 0.01) {
        var next = cellIterator.next
        val pow = List(WindPotential.powerInstalled(next), i._2 - addedPower).min
        addedPower = addedPower + pow
        addedProduction = addedProduction + pow * CapacityFactorCalculation(next) * Hours(365 * 24)
      }

      println(i._1.name + "\t" + wind.area(gr) + "\t" + i._2.toMegawatts + "\t" + addedPower.toMegawatts + "\t" + addedProduction.to(GigawattHours))

    }
  }

}