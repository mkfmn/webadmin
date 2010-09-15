morpheus.provide("morpheus.components.server.monitor.PrimitiveCountWidget");

//$.require("components/morpheus.server.monitor/js/JmxValueTracker.js");

/**
 * Used to keep track of the current disk usage.
 * 
 * @param server
 *            is the server instance to track
 * @param interval
 *            (optional) is the update interval in milliseconds. The default is
 *            10000.
 */
morpheus.components.server.monitor.DiskUsageWidget = function(server,
		interval) {

	var me = {};

	me.server = server;
	me.tracker = null;
	me.ui = $("<div class='mor_module mor_span-3'></div>");
	
	//
	// PUBLIC
	//

	me.public = {
			
		/**
		 * Render this widget.
		 * @return a ui element to insert into the DOM
		 */
		render : function() {
			
			if ( ! me.uiLoaded ) {
				me.uiLoaded = true;
				me.ui.setTemplateURL("components/morpheus.server.monitor/templates/DiskUsageWidget.tp");
			}
			
			if ( ! me.runnning ) {
				me.public.startPolling();
			}
			
			return me.ui;
		
		},
		
		stopPolling : function() {
			me.tracker.stop();
			me.running = false;
		},
		
		startPolling : function() {
			me.tracker.run();
			me.running = true;
		}
			
	};

	//
	// INTERNALS
	//

	me.extractor = function(bean) {
		var values = {};
		for( var i = 0, l = bean.attributes.length; i < l; i++ ){
			values[bean.attributes[i].name] = bean.attributes[i];
		}
		
		return values;
	};

	me.valueChanged = function(data) {
		me.data = data;
		
		if ( me.uiLoaded ) {
			me.ui.processTemplate({
				data : me.data.value
			});
		}
		
		return true;
	};

	//
	// CONSTRUCT
	// 

	me.tracker = morpheus.components.server.monitor.JmxValueTracker(me.server,
			"localkernel:Store file sizes", me.extractor, me.valueChanged,
			interval || 10000);

	return me.public;
};