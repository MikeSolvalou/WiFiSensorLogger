//suggested by http://www.recursion.org/d3-for-mere-mortals/
//derived from https://developers.google.com/chart/interactive/docs/gallery/scatterchart
//also see
// https://developers.google.com/chart/interactive/docs/quick_start
// https://developers.google.com/chart/interactive/docs/datatables_dataviews
google.charts.load('current', {'packages':['corechart']});
google.charts.setOnLoadCallback(drawChart); //set callback function to be called when google vis api is loaded

//create and populate a DataTable, then draw a chart
function drawChart(){
  //fetch data with d3-request
  d3.csv(`/data.csv?loc=${document.getElementById('locationSelector').value}`, rcf, callback);
  //TODO: time filter using url query segment


  //row conversion function for d3.csv
  function rcf(d, i, colNames){
    d.sensor = +d.sensor; //coerce sensor column to numbers
    d.timestamp = new Date((+d.timestamp)*1000);  //convert timestamp column to Date object
    d[colNames[2]] = +d[colNames[2]]; //coerce 3rd column to numbers
    return d;
  }


  //callback function for d3.csv
  // 'data' is an array of objects, one object for each row of csv data
  //  object property names = csv column names
  //  object property values = csv cell values
  // data array also has a columns property with an array of column names
  function callback(error, data){
    if(error) throw error;

    //go through data array, create Set of distinct sensor ids
    var sensorSet = new Set();
    for(var d of data)
      sensorSet.add(d.sensor);

    //create array of sensor ids, sorted numerically
    // could theoretically get this from a database query at the back end
    var sensorArray = Array.from(sensorSet).sort((a,b) => a-b); //from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/sort

    //create Map of sensor ids to incrementing integers; integer = column index in final DataTable
    var sensorMap = new Map();
    var i=1;
    for(var s of sensorArray)
      sensorMap.set(s, i++);


    //go through data array, create DataTable for scatter chart
    // google scatter chart needs a DataTable with a column for x values, and a column of y values per series
    // see https://developers.google.com/chart/interactive/docs/gallery/scatterchart#data-format

    //  column setup
    var dataTable = new google.visualization.DataTable();
    dataTable.addColumn('datetime', 'timestamp'); //datetime-type column takes a JS Date object
    for(var s of sensorArray)
      dataTable.addColumn('number', `sensor ${s}`);

    //  row data entry
    for(var d of data){
      var row = new Array(sensorSet.size+1);
      row[0] = d.timestamp;  //has been converted to JS Date
      row[sensorMap.get(d.sensor)] = d[data.columns[2]];
      dataTable.addRow(row);
    }

    //draw scatter chart
    var options = {title:'Temperature Log',
      hAxis:{title:'Time', viewWindow:{max:new Date(fromDate.value), min:new Date(toDate.value)} },
      vAxis:{title:'Temperature (C)'} };
    var chart = new google.visualization.ScatterChart(document.getElementById('chart'));
    chart.draw(dataTable, options);
  }

}

document.getElementById('updateButton').onclick = function(){
  drawChart();
}
