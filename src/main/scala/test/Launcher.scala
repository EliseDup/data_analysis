

package test

import java.io.ObjectInputStream
import java.io.FileInputStream
import org.jfree.data.category.DefaultCategoryDataset
import utils.PlotHelper
import historicalData.LoadData
import historicalData.SolarData
import historicalData.WindData
import utils.Helper
import historicalData.MeteoData

object Launcher {

  def main(args: Array[String]) = {
    val wind = Helper.readResult("wind").asInstanceOf[WindData]
    val solar = Helper.readResult("solar").asInstanceOf[SolarData]
    val load = Helper.readResult("load").asInstanceOf[LoadData]
    val meteo = Helper.readResult("meteo").asInstanceOf[MeteoData]

    def chartWindCapacityFactorPerMonth {
      val dataset = new DefaultCategoryDataset()
      wind.monthlyAverages.map(i => { dataset.addValue(i.capacityFactor, i.time.getYear, i.time.getMonthOfYear) })
      PlotHelper.barChart(dataset)
    }

    val meteo2015 = meteo.data.filter(i => i.windSpeed >= 0 && i.time.getYear == 2015 && i.time.getMonthOfYear == 10)
    val wind2015 = wind.data.filter(i => i.time.getYear == 2015 && i.time.getMonthOfYear == 10)

    //helper.plotTime(meteo2015.map(_.time), meteo2015.map(_.windSpeed),"", "Wind Speed [m/s]")
    //helper.plotTime(wind2015.map(_.time), wind2015.map(_.energy), "", "Wind Power Generation [MWh]")

    // helper.windPlot2(meteo)

    val mean = meteo.windSpeed.sum / meteo.windSpeed.size.toDouble
    println("Mean Wind Speed :" + mean)

  }
}
