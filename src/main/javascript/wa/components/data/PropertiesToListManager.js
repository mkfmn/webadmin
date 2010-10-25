
/**
 * Handles the control for what properties to list in the related nodes table in the
 * data browser.
 */
wa.components.data.PropertiesToListManager = (function($) { 
	
	var me = {};
	
	me.listFields = ['name'];
	
	//
	// INTERNALS
	//
	
	/**
	 * Set the display fields with a comma separated string.
	 */
	me.setFieldString = function(fieldString) {
		me.listFields = [];
		var fields = fieldString.split(",");
		for(var i=0,l=fields.length; i<l; i++) {
			me.listFields.push($.trim(fields[i]));
		}
		
		wa.trigger("data.listnames.changed", key);
	};
	
	// 
	// CONSTRUCT
	//
	
	$("#mor_data_listfields_button").live("click", function(ev) {
		ev.preventDefault();
		
		var fieldString = $("#mor_data_listfields").val();
		
		me.setFieldString(fieldString);
		
		// Persist the new setting
		wa.components.config.Config.set("general.data.listfields", fieldString);
	});
	
	return {
        getListFields : function() {
            return me.listFields;
        },
        
        serverChanged : function(ev) {
            wa.components.config.Config.get("general.data.listfields", function(data) {
                me.setFieldString(data.value);
            }); 
        }
    };
	
})(jQuery);


wa.bind("servers.changed", wa.components.data.PropertiesToListManager.serverChanged);