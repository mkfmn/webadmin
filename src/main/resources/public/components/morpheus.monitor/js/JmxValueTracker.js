morpheus.provide("morpheus.components.monitor.JmxValueTracker");

/**
 * Tracks a given jmx value of a given neo4j server.
 * 
 * @param server is the server instance to track
 * @param beanName is the bean to track
 * @param extractor is a function that will extract the value we want from the bean object
 * @param cb is a callback that is triggered with the new value any time the value beeing tracked changes.
 * @param interval (optional) is the update interval in milliseconds. The default is 10000.
 */
morpheus.components.monitor.JmxValueTracker = function(server, beanDomain, beanName, extractor, cb, interval) {
	
	var me = {};
	
	me.polling_interval = interval || 10000;
	me.max_polling_interval = me.polling_interval * 10;

	/**
	 * This is the actual current polling interval beeing used.
	 * If the server data has not changed, the polling time will be
	 * extended until it reaches max_polling interval.
	 */
	me.actual_polling_interval = me.polling_interval;
	
	me.callback = cb;
	me.extractor = extractor;
	me.beanDomain = beanDomain;
	me.beanName = beanName;
	me.jmx = server.manage.jmx;
	
	me.prevValue = null;
	
	//
	// PUBLIC
	//
	
	me.api = {
		run : function() {
			me.poll();
		},
		
		stop : function() {
			me.stopIssued = true;
		}
	};
	
	//
	// INTERNALS
	//
	
	me.poll = function() {
		me.jmx.getBean(beanDomain, beanName, function(beans) {
			if( beans ) {
				var value = me.extractor(beans[0]);
				if ( value !== me.prevValue ) {
					me.prevValue = value;
					var keepPolling = me.callback({ value:value, bean:beans[0], beanName:me.beanName });
					
					if( keepPolling ){
						
						// Reset the polling interval
						me.actual_polling_interval = me.polling_interval;
						
					} else {
						return;
					}
				} else {
					if( me.actual_polling_interval >= me.max_polling_interval ) {
						me.actual_polling_interval = me.max_polling_interval;
					} else {
						me.actual_polling_interval *= 2;
					}
				}
			}
			
			if ( ! me.stopIssued ) {
				setTimeout(me.poll, me.actual_polling_interval);
			}
		});
		
	};
	
	//
	// CONSTRUCT
	// 
	
	return me.api;
};