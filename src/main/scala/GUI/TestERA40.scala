package landCover

import utils.PlotHelper
import utils.Helper
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVWriter
import utils.GeoPoint
import squants.space._
import squants.energy._
import utils._
import squants.time.Hours
import operation._
import construction._

object TestERA40 {
  
  def main(args: Array[String]) = {

    val t = new WindTurbine2MW
    t.plot
    t.plotCurve
   println("---")
    //Helper.txtToCSV(Helper.ressourcesPy + "results/worldDailyAugust2002test", Helper.ressourcesPy + "test.csv", List(4,3,2),true)

    val name = "world5years"
    val wind = new GridData("results/" + name, Degrees(0.25), new WindTurbine2MW, new WindTurbine2MW)

    println(name + "\t" + "Grid Size : " + wind.grids.size + "=" + wind.grids.map(_.area).foldLeft(SquareKilometers(0))(_ plus _))
    wind.plotEROIVSCumulatedProduction(wind.landGrids())
    PlotHelper.cumulativeDensity(List((wind.windSpeeds(), "10 metres"), (wind.windSpeeds(atHub=true), "80-120 metres")), xLabel = "% of Sites", yLabel = "Mean Wind Speed [m/s]")
    // PlotHelper.cumulativeDensity(List((wind.powerDensities(), "10 metres"), (wind.powerDensities80(), "80 metres")), xLabel = "% of Sites", yLabel = "Power Density [W/m^2]")
    PlotHelper.cumulativeDensity(List((wind.erois(wind.landGrids()), "")), xLabel = "% of Sites", yLabel = "EROI")

  }
}