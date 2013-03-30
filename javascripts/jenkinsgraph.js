var json;
var viz;
var updateCenter = {};
updateCenter.post = function(jsonresponse) {
   json = jsonresponse;
//$(document).ready(function() {
	
	// $.ajaxSetup({'dataType' : 'text'});
	// $.get('http://mirrors.karan.org/jenkins/updates/update-center.json', function(data) {
		// var trimmed = data.substring(data.indexOf('{'));
		// trimmed = trimmed.substring(0, trimmed.lastIndexOf('}') + 1);
		// json = $.parseJSON(trimmed);
	    
	    $('#jenkinsdep').css({width: $(window).innerWidth() , height: $(window).innerHeight()});	
		var chartdata = getJsonForChart(window.filter); // set filter default 
		var chartjson = chartdata.json;
		var noofdeps = chartdata.noofdeps;
		
		
//		alert('FOUND ' + chartjson.length + ' PLUGINS WITH ' + noofdeps + ' DEPENDENCIES');
		
		$('#loadinfo').html(chartjson.length + ' plugins with ' + noofdeps + ' dependencies');
		
		viz = new $jit.ForceDirected({
		    //id of the visualization container
		    injectInto: 'jenkinsdep',
		    //Enable zooming and panning
		    //by scrolling and DnD
		    Navigation: {
		      enable: true,
		      //Enable panning events only if we're dragging the empty
		      //canvas (and not a node).
		      panning: 'avoid nodes',
		      zooming: 10 //zoom speed. higher is more sensible
		    },
		    // Change node and edge styles such as
		    // color and width.
		    // These properties are also set per node
		    // with dollar prefixed data-properties in the
		    // JSON structure.
		    Node: {
		      overridable: true
		    },
		    Margin: {  
		    	top: 20,  
		    	left: 20,  
		    	right: 20,  
		    	bottom: 20  
		    },
		    Edge: {
		      overridable: true,
		      color: '#23A4FF',
		      lineWidth: 0.4,
		      type: 'arrow'
		    },
		    //Native canvas text styling
		    Label: {
		      type: 'Native', //Native or HTML
		      size: 10,
		      style: 'bold'
		    },
		    //Add Tips
		    Tips: {
		      enable: true,
		      onShow: function(tip, node) {
		        //count connections
		        var count = 0;
		        node.eachAdjacency(function() { count++; });
		        //display node info in tooltip
		        var html = "<div class=\"tip-title\">" + node.name + "</div>";
//		          + "<div class=\"tip-text\"><b>dependencies:</b> " + count + "</div>";
		        var pluginJSON = json.plugins[node.id];
		        if(pluginJSON) {
		        	html += "<div class=\"tip-text\" style='margin-bottom: 5px;'><b>Excerpt:</b> " + pluginJSON.excerpt + "</div>";
		        	html += "<div class=\"tip-text\"><b>Version:</b> " + pluginJSON.version + "</div>";
		        	html += "<div class=\"tip-text\"><b>Build Date:</b> " + pluginJSON.buildDate + "</div>";
		        	html += "<div class=\"tip-text\"><b>Required Core:</b> " + pluginJSON.requiredCore + "</div>";
		        	
		        	html += "<div class=\"tip-text\" style='margin-top: 5px;'><b>Developers:</b> ";
		        	for(var i = 0; i < pluginJSON.developers.length; i++) {
		        		html += pluginJSON.developers[i].name + " (" + pluginJSON.developers[i].developerId + ")";
		        		if(i < pluginJSON.developers.length -1) {
		        			html += ", ";
		        		}
		        	}
		        	html += "</div>";
		        	
		        	if(pluginJSON.dependencies.length > 0) {
		        		html += "<div class=\"tip-text\" style='margin-top: 5px;'><b>Dependencies:</b><ul>";
			        	for(var i = 0; i < pluginJSON.dependencies.length; i++) {
			        		html += "<li>";
			        		var dependencyJSON = json.plugins[pluginJSON.dependencies[i].name];
			        		if(dependencyJSON) {
			        			html += dependencyJSON.title;
			        		} else {
			        			html += pluginJSON.dependencies[i].name;
			        		}
			        		html += " (version: " + pluginJSON.dependencies[i].version + " / optional: " + pluginJSON.dependencies[i].optional + ")"
			        		html += "</li>";
			        	}
			        	html += "</ul></div>";
		        	}
		        	var i = 0;
		        }
		        tip.innerHTML = html;
		      }
		    },
		    // Add node events
		    Events: {
		      enable: true,
		      type: 'Native',
		      //Change cursor style when hovering a node
		      onMouseEnter: function() {
		        viz.canvas.getElement().style.cursor = 'move';
		      },
		      onMouseLeave: function() {
		        viz.canvas.getElement().style.cursor = '';
		      },
		      //Update node positions when dragged
		      onDragMove: function(node, eventInfo, e) {
		          var pos = eventInfo.getPos();
		          node.pos.setc(pos.x, pos.y);
		          viz.plot();
		      },
		      //Implement the same handler for touchscreens
		      onTouchMove: function(node, eventInfo, e) {
		        $jit.util.event.stop(e); //stop default touchmove event
		        this.onDragMove(node, eventInfo, e);
		      },
		      //Add also a click handler to nodes
		      onClick: function(node) {
		        if(!node) return;
		        if(node.id == 'Jenkins Core') {
		            filterGraph();
		        } else {
		        	buildGraphForNode(node.id);    
		        }
		        
	        // Build the right column relations list.
	        // This is done by traversing the clicked node connections.
		        // var html = "<h4>" + node.name + "</h4><b> dependencies:</b><ul><li>",
		            // list = [];
		        // node.eachAdjacency(function(adj){
		          // list.push(adj.nodeTo.name);
		        // });
		        // //append connections information
		        // $jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul>";
		      }
		    },
		    //Number of iterations for the FD algorithm
		    iterations: 20,
		    //Edge length
		    levelDistance: 150,
		    // Add text to the labels. This method is only triggered
		    // on label creation and only for DOM labels (not native canvas ones).
//		    onCreateLabel: function(domElement, node){
//		      domElement.innerHTML = node.name;
//		      var style = domElement.style;
//		      style.fontSize = "0.8em";
//		      style.color = "#ddd";
//		    },
		    // Change node styles when DOM labels are placed
		    // or moved.
		    onPlaceLabel: function(domElement, node){
//		      var style = domElement.style;
//		      var left = parseInt(style.left);
//		      var top = parseInt(style.top);
//		      var w = domElement.offsetWidth;
//		      style.left = (left - w / 2) + 'px';
//		      style.top = (top + 10) + 'px';
//		      style.display = '';
		    }
		  });
		  // load JSON data.
//		  fd.loadJSON(chartjson.slice(0,25));
		  viz.loadJSON(chartjson);
		  var percdone = $('#percdone');
		  // compute positions incrementally and animate.
		  viz.computeIncremental({
		    iter: 1,
		    property: 'end',
		    onStep: function(perc){
		    	percdone.html(perc + ' %');
//		      Log.write(perc + '% loaded...');
		    },
		    onComplete: function(){
		    	percdone.html('done');
		    	setTimeout(function(){
		    		$('#percdonecontainer').fadeOut();
		    	}, 1000);
//		      Log.write('done');
		      viz.animate();
//		      fd.animate({
//		        modes: ['linear'],
//		        transition: $jit.Trans.Elastic.easeOut,
//		        duration: 2500
//		      });
		    }
		  });
//});
// });
}

function getJsonForChart(filter) {
	var chartjson = new Array();
	var noofdeps = 0;
	
	var filterRegexp;
	if(filter && $.trim(filter) != '') {
		filterRegexp = new RegExp($.trim(filter), 'i');
	} 
	
	for(var plugin in json.plugins) {
		var pluginJSON = json.plugins[plugin];
		if(!filterRegexp || pluginJSON.name.match(filterRegexp) || pluginJSON.title.match(filterRegexp) || containsDeveloper(pluginJSON, filterRegexp)) {
			var pluginNode = {
				'id' : pluginJSON.name,
				'name' : pluginJSON.title,
				'data': {
					'$color': '#83548B',
			        '$type': 'circle',
			        '$dim': 10
				},
			    'adjacencies' : new Array()
			};
			
			pluginNode.adjacencies.push({
			    'nodeTo': 'Jenkins Core',
                'nodeFrom': pluginJSON.name,
                'data': {
                    '$color': '#AAAAAA',
                    "$direction": [pluginJSON.name, 'Jenkins Core']
                }
			});
			
			for(var i = 0; i < pluginJSON.dependencies.length; i++) {
				noofdeps++;
				pluginNode.adjacencies.push({
					'nodeTo': pluginJSON.dependencies[i].name,
					'nodeFrom': pluginJSON.name,
					'data': {
						'$color': '#AAAAAA',
						"$direction": [pluginJSON.name, pluginJSON.dependencies[i].name]
		            }
				});
			}
			chartjson.push(pluginNode);
		}
//		$('body').append('<div>' + plugin + '</div>');
	}
	return {json: chartjson, noofdeps: noofdeps};
}

function containsDeveloper(pluginJSON, filterRegexp) {
    if(pluginJSON.developers && pluginJSON.developers.length > 0)  {
        for(var i = 0; i < pluginJSON.developers.length; i++) {
            var developer = pluginJSON.developers[i];
            if((developer.developerId && developer.developerId.match(filterRegexp)) || (developer.name && developer.name.match(filterRegexp))) {
                return true;
            }
        }
    }
    return false;
}

function filterGraph(filter) {
	var chartdata = getJsonForChart(filter);
	var chartjson = chartdata.json;
	var noofdeps = chartdata.noofdeps;
	
	$('#loadinfo').html(chartjson.length + ' plugins with ' + noofdeps + ' dependencies');
	$('#percdonecontainer').fadeIn();
	var percdone = $('#percdone');
	// if(chartjson.length < 50) {
		// viz.config.iterations = 10;
	// } else {
		// viz.config.iterations = 5;
	// }
	
	viz.loadJSON(chartjson);
	viz.computeIncremental({
	    iter: 1,
	    property: 'end',
	    onStep: function(perc){
	    	percdone.html(perc + ' %');
//	      Log.write(perc + '% loaded...');
	    },
	    onComplete: function(){
	    	percdone.html('done');
	    	setTimeout(function(){
	    		$('#percdonecontainer').fadeOut();
	    	}, 1000);
	    	viz.animate();
	    }
	});
}

function getJSONForNode(nodeid) {
    var chartjson = new Array();
    var noofdeps = 0;
    
    for(var plugin in json.plugins) {
        var pluginJSON = json.plugins[plugin];
        if(pluginJSON.name == nodeid) {
            var pluginNode = {
                'id' : pluginJSON.name,
                'name' : pluginJSON.title,
                'data': {
                    '$color': '#83548B',
                    '$type': 'circle',
                    '$dim': 10
                },
                'adjacencies' : new Array()
            };
            
            pluginNode.adjacencies.push({
                'nodeTo': 'Jenkins Core',
                'nodeFrom': pluginJSON.name,
                'data': {
                    '$color': '#AAAAAA',
                    "$direction": [pluginJSON.name, 'Jenkins Core']
                }
            });
            
            for(var i = 0; i < pluginJSON.dependencies.length; i++) {
                noofdeps++;
                pluginNode.adjacencies.push({
                    'nodeTo': pluginJSON.dependencies[i].name,
                    'nodeFrom': pluginJSON.name,
                    'data': {
                        '$color': '#AAAAAA',
                        "$direction": [pluginJSON.name, pluginJSON.dependencies[i].name]
                    }
                });
            }
            
            chartjson.push(pluginNode);
        } else {
            for(var i = 0; i < pluginJSON.dependencies.length; i++) {
                if(pluginJSON.dependencies[i].name == nodeid) {
                    var pluginNode = {
                        'id' : pluginJSON.name,
                        'name' : pluginJSON.title,
                        'data': {
                            '$color': '#83548B',
                            '$type': 'circle',
                            '$dim': 10
                        },
                        'adjacencies' : new Array()
                    };
                    
                    pluginNode.adjacencies.push({
                        'nodeTo': 'Jenkins Core',
                        'nodeFrom': pluginJSON.name,
                        'data': {
                            '$color': '#AAAAAA',
                            "$direction": [pluginJSON.name, 'Jenkins Core']
                        }
                    });
                    
                    pluginNode.adjacencies.push({
                        'nodeTo': pluginJSON.dependencies[i].name,
                        'nodeFrom': pluginJSON.name,
                        'data': {
                            '$color': '#AAAAAA',
                            "$direction": [pluginJSON.name, pluginJSON.dependencies[i].name]
                        }
                    });
                    chartjson.push(pluginNode);
                }
            }
        }
//      $('body').append('<div>' + plugin + '</div>');
    }
    return {json: chartjson, noofdeps: noofdeps};
}

function buildGraphForNode(nodeid) {
    var chartdata = getJSONForNode(nodeid);
    var chartjson = chartdata.json;
    var noofdeps = chartdata.noofdeps;
    
    $('#loadinfo').html(chartjson.length + ' plugins with ' + noofdeps + ' dependencies');
    $('#percdonecontainer').fadeIn();
    var percdone = $('#percdone');
    // if(chartjson.length < 50) {
        // viz.config.iterations = 10;
    // } else {
        // viz.config.iterations = 5;
    // }
    
    viz.loadJSON(chartjson);
    viz.computeIncremental({
        iter: 1,
        property: 'end',
        onStep: function(perc){
            percdone.html(perc + ' %');
//        Log.write(perc + '% loaded...');
        },
        onComplete: function(){
            percdone.html('done');
            setTimeout(function(){
                $('#percdonecontainer').fadeOut();
            }, 1000);
            viz.animate();
        }
    });
}
