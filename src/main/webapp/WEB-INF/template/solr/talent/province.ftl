<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8" />
	<title>数据统计</title>
</head>

<body>

	<div id="content-header">
		<div id="breadcrumb">
			<a href="#" class="tip-bottom"> <i class="icon-home"></i>
				Home
			</a>
			<a href="#" class="tip-bottom"> <i class="icon-bar-chart"></i>
				数据统计
			</a>
			<a href="#" class="current">用户分布统计</a>
		</div>
		<h1>用户分布统计</h1>
	</div>
	<div class="container-fluid">
		<hr>
		<div class="row-fluid">
			<div class="span12">
				<div class="widget-box">
					<div class="widget-title">
						<span class="icon">
							<i class="icon-signal"></i>
						</span>
						<h5>用户分布统计(总数：${total}人)</h5>
					</div>
					<div class="widget-content">
						<div class="bars"></div>
					</div>
				</div>
			</div>
		</div>

	</div>

	<script src="/common/matrix/js/jquery.flot.min.js"></script>
	<script src="/common/matrix/js/jquery.flot.pie.min.js"></script>
	<script src="/common/matrix/js/jquery.flot.resize.min.js"></script>
	<script src="/common/matrix/js/jquery.flot.axislabels.js"></script>
	<script src="/common/matrix/js/jquery.peity.min.js"></script>
	<script type="text/javascript">
	<!--Bar-chart-js-->
	var barData = new Array(); //数据  
    var ticks = new Array(); //横坐标值  
    <#list barData as list>
    	barData.push([${list_index},'${list.count}']);
    	ticks.push([${list_index},'${list.name}'])
    </#list>
	var dataset = [{label: "用户分布", data: barData, color: "#2E363F" }];
	
	var options = {
        series: {
            bars: {
                show: true
            }
        },
        bars: {
            align: "center",
            barWidth: 0.5
        },
        xaxis: {
            axisLabel: "省份",
            axisLabelUseCanvas: true,
            axisLabelFontSizePixels: 12,
            axisLabelFontFamily: 'Verdana, Arial',
            axisLabelPadding: 10,
            ticks: ticks
        },
        yaxis: {
            axisLabel: "用户数",
            axisLabelUseCanvas: true,
            axisLabelFontSizePixels: 12,
            axisLabelFontFamily: 'Verdana, Arial',
            axisLabelPadding: 3,
            tickFormatter: function (v, axis) {
                return v + "人";
            }
        },
        legend: {
            noColumns: 0,
            //labelBoxBorderColor: "#000000",
            position: "ne"
        },
        labelFormatter: function(label, series) {
		    // series is the series object for the label
		    return '<a href="#' + label + '">' + label + '</a>';
		},
        grid: {
            hoverable: true,
            borderWidth: 2,
            backgroundColor: { colors: ["#ffffff", "#EDF5FF"] }
        }
    };
    
	$(document).ready(function () {
	    //Display graph
	    $.plot($(".bars"), dataset, options, {
			legend: true
		});
		$(".bars").UseTooltip();
    });
    
    var previousPoint = null, previousLabel = null;
	$.fn.UseTooltip = function () {
	        $(this).bind("plothover", function (event, pos, item) {
	            if (item) {
	                if ((previousLabel != item.series.label) || (previousPoint != item.dataIndex)) {
	                    previousPoint = item.dataIndex;
	                    previousLabel = item.series.label;
	                    $("#tooltip").remove();
	
	                    var x = item.datapoint[0];
	                    var y = item.datapoint[1];
	                    var color = item.series.color;
	
	                    //console.log(item.series.xaxis.ticks[x].label);                
	
	                    showTooltip(item.pageX,
		                    item.pageY,
		                    color,
		                    "<strong>" + item.series.label + "</strong><br>" + item.series.xaxis.ticks[x].label + " : <strong>" + y + "</strong> 人");
		                }
	            } else {
	                $("#tooltip").remove();
	                previousPoint = null;
	            }
	        });
	    };

    function showTooltip(x, y, color, contents) {
        $('<div id="tooltip">' + contents + '</div>').css({
            position: 'absolute',
            display: 'none',
            top: y - 40,
            left: x - 120,
            border: '2px solid ' + color,
            padding: '3px',
            'font-size': '9px',
            'border-radius': '5px',
            'background-color': '#fff',
            'font-family': 'Verdana, Arial, Helvetica, Tahoma, sans-serif',
            color: color,
            opacity: 0.9
        }).appendTo("body").fadeIn(200);
    }
	<!--Bar-chart-js-->	
	</script>
</body>
</html>