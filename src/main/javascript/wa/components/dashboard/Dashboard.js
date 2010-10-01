
/**
 * Base module for the monitor component.
 */
wa.components.dashboard.Dashboard = (function($, undefined) {
    
    var me = {};
    
    me.basePage = $("<div></div>");
    me.ui = {};
    
    me.uiLoaded  = false;
    me.server = null;
    
    me.visible = false;
    
    me.valueTrackers = [];
    me.charts = [];
    
    //
    // PUBLIC
    //
    
    me.api = {
            
            getPage :  function() {
                return me.basePage;
            },
            
            pageChanged : function(ev) {
                if(ev.data === "dashboard") {
            		me.visible = true;
                
                    if( me.uiLoaded === false ) {
                    	me.uiLoaded = true;
                        me.basePage.setTemplateURL("templates/components/monitor/index.tp");
	                    
                        me.server = wa.Servers.getCurrentServer();
                        
                        if(me.server) {
                            me.reload();
                        }
                    } else {
                    	me.runMonitors();
                    }
                	
                } else {
                    me.visible = false;
                    me.haltMonitors();
                }
            },
            
            serverChanged : function(ev) {
                
                if( me.server != wa.Servers.getCurrentServer() ) {
                    me.server = wa.Servers.getCurrentServer();
                    // If the monitor page is currently visible
                    if( me.visible === true ) {
                    	me.reload();
                    }
                }
            },
            
            init : function() { }
            
    };
    
    // 
    // PRIVATE
    //
    
    me.reload = function() {
        
        me.basePage.processTemplate({
            server : me.server
        });
        
        me.destroyMonitors();
        
        var server = wa.Servers.getCurrentServer();
        
        if( server ) {
        	me.loadMonitors(server);
        	$("#mor_monitor_lifecycle").empty();
        	$("#mor_monitor_lifecycle").append( wa.widgets.LifecycleWidget(server).render() );
        }
        
    };
    
    me.destroyMonitors= function() {
    	me.haltMonitors();
    	
    	me.valueTrackers = [];
    	me.charts = [];
    };
    
    me.haltMonitors = function() {
    	for( var i = 0, l = me.charts.length; i < l ; i++ ) {
			me.charts[i].stopDrawing();
    	}
    	
    	for( var i = 0, l = me.valueTrackers.length; i < l ; i++ ) {
			me.valueTrackers[i].stopPolling();
    	}
    };
    
    me.runMonitors = function() {
    	for( var i = 0, l = me.charts.length; i < l ; i++ ) {
			me.charts[i].startDrawing();
    	}
    	
    	for( var i = 0, l = me.valueTrackers.length; i < l ; i++ ) {
			me.valueTrackers[i].startPolling();
    	}
    };
    
    me.loadMonitors = function(server) {
    	var box = $("#mor_monitor_valuetrackers");
    	
    	var primitiveTracker = wa.components.dashboard.PrimitiveCountWidget(server);
    	var diskTracker      = wa.components.dashboard.DiskUsageWidget(server);
    	var cacheTracker      = wa.components.dashboard.CacheWidget(server);
    	
    	var primitivesChart = wa.components.dashboard.MonitorChart(server, {
    		label : 'Primitive entitites',
    		data : {
    		    node_count : {
				    label : 'Nodes'
				},
				relationship_count : {
				    label : 'Relationships'
			    },
			    property_count : {
				    label : 'Properties'
			    }
    		}
    	});
    	
    	var memoryChart = wa.components.dashboard.MonitorChart(server, {
    		label : 'Heap memory usage',
    		data : {
    			memory_usage_percent : {
				    label : 'Heap memory usage',
				}
    		},
    		yaxis : {
    			min : 0,
    			max : 100
    		},
    		series : {
    			lines: { show: true, fill: true, fillColor: "#4f848f" }
    		},
    		tooltipValueFormatter : function(v) {
    			return Math.floor(v) + "%";
    		}
    	});
    	
    	
    	
    	me.valueTrackers.push(primitiveTracker);
    	me.valueTrackers.push(diskTracker);
    	me.valueTrackers.push(cacheTracker);
    	
    	me.charts.push(primitivesChart);
    	me.charts.push(memoryChart);

    	box.append(primitivesChart.render());
    	box.append(memoryChart.render());
    	box.append(primitiveTracker.render());
    	box.append(diskTracker.render());
    	box.append(cacheTracker.render());
    	
    	primitivesChart.startDrawing();
    	memoryChart.startDrawing();
    };
    
    //
    // CONSTRUCT
    //
    
    $('.mor_monitor_showjmx').live('click', function(ev) {
        
        $.bbq.pushState({
            p :"morpheus.monitor.jmx"
        });
    
        ev.preventDefault();
    });
    
    return me.api;
    
})(jQuery);

//
// REGISTER STUFF
//

wa.ui.Pages.add("dashboard",wa.components.dashboard.Dashboard);
wa.ui.MainMenu.add({ label : "Dashboard", pageKey:"dashboard", index:0, requiredServices:['monitor'], perspectives:['server']});

wa.bind("init", wa.components.dashboard.Dashboard.init);
wa.bind("ui.page.changed", wa.components.dashboard.Dashboard.pageChanged);
wa.bind("servers.current.changed",  wa.components.dashboard.Dashboard.serverChanged);