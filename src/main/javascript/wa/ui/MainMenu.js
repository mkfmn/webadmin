wa.ui.MainMenu = ( function( $, undefined )
{

    var me = {};

    //
    // PRIVATE
    //

    me.container = null;

    me.currentPage = null;
    me.menuItems = [];
    
    me.perspective = "";

    me.init = function()
    {
        me.container = $( "#mor_mainmenu" );

        me.container.setTemplateURL( "templates/mainmenu.tp" );
        
        me.render();
        
        $( window ).bind( "hashchange", me.hashchange );
    };

    /**
     * Re-render the main menu
     */
    me.render = function()
    {
        
        if( me.container !== null ) {
            
            var currentServices = [];
            if( wa.Servers.getCurrentServer() ) {
                if(!wa.Servers.getCurrentServer().manage.servicesLoaded()) {
                    wa.Servers.getCurrentServer().bind("services.loaded",me.render);
                } else {
                    currentServices = wa.Servers.getCurrentServer().manage.availableServices();
                }
            }
                
            var item, items = [];
            for( var key in me.menuItems ) {
                item = me.menuItems[key];
                
                if( me.itemRequirementsFulfilled(item, currentServices) ) {
                    items.push(item);
                }
            }
            
            me.container.processTemplate(
            {
                items : items,
                urlAppend : me.getExtraUrlParams()
            });
        }
    };
    
    /**
     * Check if the current state matches what a given item wants in order to be
     * shown.
     */
    me.itemRequirementsFulfilled = function(item, currentServices) {
        // Ensure perspective is correct for this item
        if( $.inArray("all", item.perspectives) !== -1 || $.inArray(me.perspective, item.perspectives) !== -1 ) {
            
            // Ensure required services are available
            if(item.requiredServices.length > 0) {
                for(var index in item.requiredServices) {
                    if( $.inArray( item.requiredServices[index], currentServices ) === -1 ) {
                        return false;
                    }
                }
            }
            
            return true;
        }
        
        return false;
    };

    /**
     * Set the current page beeing viewed.
     */
    me.setCurrentPage = function( currentPage )
    {

        for ( var i = 0, l = me.menuItems.length; i < l; i++ )
        {
            if ( me.menuItems[i].page === currentPage )
            {
                me.menuItems[i].isCurrent = true;
            }
            else
            {
                me.menuItems[i].isCurrent = false;
            }
        }

        
        me.render();
        
    };
    
    me.update = function( name, update ) {
      
        for(var key in me.menuItems) {
            if(me.menuItems[key].name === name) {
                
                for(var param in update) {
                    me.menuItems[key][param] = update[param];
                }
                
            }
        }
        
        me.render();
        
    };

    me.getSetClass = function(set) {
        return "menuset-" + set;
    };
    
    me.hashchange = function(ev) {
      
        me.render();
        
    };
    
    /**
     * Get a string of all url hash parameters, except the page one. This is
     * appended to the end of all menu links each time the hash changes.
     */
    me.getExtraUrlParams= function() {
      
        var data = $.deparam.fragment();
        delete(data.p);
        
        return "&" + $.param.fragment("", data).substring(1); 
        
    };
    
    wa.bind("servers.current.changed", function(ev) {
        if(ev.data.current) {
            wa.ui.MainMenu.setPerspective("server");
        }
    });
    
    //
    // PUBLIC API
    //

    return {
        init : me.init,
        
        /**
         * Add a new menu item.
         * 
         * @param args
         *            is a dictionary with arguments.
         * 
         * @param args.label
         *            {String} the label to show in the menu
         * @param args.pageKey
         *            {String} is a key that maps to a page registered in
         *            wa.ui.Pages
         * @param args.perspectives
         *            {Array} (optional) list of perspectives where this item
         *            should be visible. Default is "all".
         * @param args.requiredServices
         *            {Array} (optional) list of services that need to be
         *            provided by the current server in order for this item to
         *            be visible.
         * 
         * Each item is a service key string mapping to the keys provided by
         * neo4j REST servers when doing a HTTP GET request to them.
         * 
         * Adding any strings here implies the item will only be shown when
         * webadmin is in the "server" perspective.
         * 
         * @param args.index
         *            {Integer} (optional) used to set where in the menu the
         *            item should be relative to other items. The higher the
         *            number, the farther to the right.
         * 
         */
        add : function( args )
        {
            var label = args.label;
            var page = args.pageKey;
            
            var data = args.data || {};
            var perspectives = args.perspectives || ["all"];
            var requiredServices = args.requiredServices || [];
            var index = args.index || 0;
            
            // Find the index where we should insert the new item
            var countdown = me.menuItems.length;
            
            while(me.menuItems[countdown - 1] != undefined && me.menuItems[countdown - 1].index > index) {
                countdown--;
            }
            
            // Insert the new item
            me.menuItems.splice(countdown, 0,
            {
                label : label,
                page : page,
                data : data,
                perspectives  : perspectives,
                requiredServices : requiredServices,
                urlAppend : "",
                index : index
            } );

            me.render();

        },
        
        /**
         * Update a menu item
         * 
         * @param [string]
         *            name of the menu item to update
         * @param [object]
         *            is a dictionary with keys corresponding to the parameters
         *            of the add method
         */
        update : me.update,
        
        setCurrentPage : me.setCurrentPage,
        
        setPerspective : function(perspective) {
            if( me.perspective !== perspective ) {
                me.perspective = perspective;
                me.render();
            }
        }
    };

} )( jQuery );