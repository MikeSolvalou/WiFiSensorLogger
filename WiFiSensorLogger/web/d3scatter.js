//outline: initial setup, call update function, define update function, define helper functions for update, define widget handlers

//initial setup
// convert Date object to string compatible with <input type=datetime-local> HTML element
function dateToString(date){
  return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}T${pad(date.getHours())}\
:${pad(date.getMinutes())}`;

  //convert number n to string, pad with 0 to ensure length is at least 2
  // from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString
  function pad(n){
    if(n<10)
      return '0'+n;
    else
      return n.toString();
  }
}


// chart margins, d3 convention - https://bl.ocks.org/mbostock/3019563
var margin = {top: 40, right: 20, bottom: 80, left: 80};
var innerWidth = document.getElementById('container').getAttribute('width') - margin.left - margin.right;
var innerHeight = document.getElementById('container').getAttribute('height') - margin.top - margin.bottom;

// d3 v4 reference - https://github.com/d3/d3/blob/master/API.md

// scales, range setup
var x = d3.scaleTime().rangeRound([0, innerWidth]); //either scaleTime or scaleUtc works, since domain values come from unix timestamps
var y = d3.scaleLinear().rangeRound([innerHeight, 0]);


var color = d3.scaleOrdinal(d3.schemeCategory10); //color scale setup

var chart = d3.select('#container').append('g') //append chart g element
    .attr('transform', `translate(${margin.left}, ${margin.top})`);


//update chart according to widgets
update();


//fetch any required data and redraw chart
function update(){
  //using d3-request for AJAX; reference - https://github.com/d3/d3-request
  d3.csv(`/data.csv?loc=${document.getElementById('locationSelector').value}`, rcf, callback);
  //TODO: time filter using url query segment
}


//helper functions

//row conversion function for d3.csv
function rcf(d, i, colNames){
  d.sensor = +d.sensor; //coerce sensor column to numbers
  d.timestamp = new Date((+d.timestamp)*1000);  //convert timestamp column to Date object
  d[colNames[2]] = +d[colNames[2]]; //coerce 3rd column to numbers
  return d;
}

//callback function for d3.csv(); draw chart with received data
// 'data' is an array of objects, one object for each row of csv data
//  object property names ~ csv column names
//  object property values ~ csv cell values
// data array also has a columns property with an array of column names
function callback(error, data){
  if(error) throw error;

  //erase chart graphics
  chart.selectAll('*').remove();

  //scales, domain setup
  var fromDate = document.getElementById('fromDate');
  var toDate = document.getElementById('toDate');
  x.domain([ new Date(fromDate.value), new Date(toDate.value) ]);
  y.domain([ d3.min(data, d => d[data.columns[2]]), d3.max(data, d => d[data.columns[2]]) ]);

  //draw axes
  chart.append('g')
      .attr('id', 'xAxis')
      .attr('transform', `translate(0, ${innerHeight})`)
      .call(d3.axisBottom(x)  //default tick settings seem okay
                /*.ticks(d3.utcMinute.every(15)) //a tick every 15 mins, utc to avoid DST issues?
                .tickFormat(d3.timeFormat('%H:%M'))*/ )  //d3.utcFormat() shows time axis in UTC instead of local
      .attr('text-anchor', 'end')
    .selectAll('text')  //select all tick labels
      .attr('transform', 'rotate(-90)')
      .attr('y', 0)
      .attr('x', -10)
      .attr('dy', '0.7ex');

  chart.append('g')
      .attr('id', 'yAxis')
      .call(d3.axisLeft(y));  //default axis settings okay

  //draw axis titles
  chart.append('text')  //y axis title
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('x', -innerHeight/2)
      .attr('y', -40)
      .text('Temperature (C)');

  chart.append('text')  //x axis title
      .attr('text-anchor', 'middle')
      .attr('x', innerWidth/2)
      .attr('y', innerHeight + 60)
      .attr('dy', '0.7ex')
      .text('Time');

  //draw chart title
  chart.append('text')
      .attr('text-anchor', 'middle')
      .attr('x', innerWidth/2)
      .attr('dy', '0.7ex')
      .text('Temperature Log');


  //draw data points
  chart.append('g')
      .attr('id', 'points')
    .selectAll('circle')
      .data(data)
    .enter().append('circle')
      .attr('cx', d => x(d.timestamp))
      .attr('cy', d => y(d[data.columns[2]]))
      .attr('r', 2)
      .style('fill', d => color(d.sensor))
    .append('title') //text to appear when hovering over circles
      .text(d => `${d.timestamp}, ${d[data.columns[2]]} C`);
}


document.getElementById('updateButton').onclick = function(){
  update();
}
