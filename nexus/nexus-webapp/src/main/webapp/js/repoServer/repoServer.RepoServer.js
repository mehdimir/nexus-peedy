/*
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
(function(){

// Repository main Controller(conglomerate) Singleton
Sonatype.repoServer.RepoServer = function(){
  var cfg = Sonatype.config.repos;
  var sp = Sonatype.lib.Permissions;

// ************************************  
  return {
    pomDepTmpl : new Ext.XTemplate('<dependency><groupId>{groupId}</groupId><artifactId>{artifactId}</artifactId><version>{version}</version></dependency>'),
    
    loginFormConfig : { 
      labelAlign: 'right',
      labelWidth:60,
      frame:true,  

      defaultType:'textfield',
      monitorValid:true,

      items:[
        { 
          id:'usernamefield',
          fieldLabel:'Username', 
          name:'username',
          tabIndex: 1,
          width: 200,
          allowBlank:false 
        },
        { 
          id:'passwordfield',
          fieldLabel:'Password', 
          name:'password',
          tabIndex: 2, 
          inputType:'password', 
          width: 200,
          allowBlank:false 
        }
      ]
      //buttons added later to provide scope to handler
       
    },
    
    buildRecoveryText : function(){
        var htmlString = null;
        
        if(sp.checkPermission('nexus:usersforgotid', sp.CREATE)){
          htmlString = 'Forgot your <a id="recover-username" href="#">username</a>'
        }
        if(sp.checkPermission('nexus:usersforgotpw', sp.CREATE)){
          if (htmlString != null){
            htmlString += ' or ';
          }
          else{
            htmlString = 'Forgot your ';
          }
          htmlString += '<a id="recover-password" href="#">password</a>';
        }
        if (htmlString != null){
          htmlString += '?';
        }
        
        return htmlString;
    },
    
    statusComplete : function( statusResponse ){        
        this.resetMainTabPanel();
        
        this.createSubComponents(); //update left panel
        
        var htmlString = this.buildRecoveryText();
        
        var recoveryPanel = this.loginForm.findById('recovery-panel');
        if ( recoveryPanel ) {
          this.loginForm.remove( recoveryPanel );
          recoveryPanel.destroy();
        }
        this.loginForm.add({
            xtype: 'panel',
            id: 'recovery-panel',
            style: 'padding-left: 70px',
            html: htmlString
          });
    },
    
    // Each Sonatype server will need one of these 
    initServerTab : function() {

      Sonatype.Events.addListener( 'nexusNavigationInit',
        this.addNexusNavigationItems, this );
      
      // Left Panel
      this.nexusPanel = new Sonatype.navigation.NavigationPanel({
        id: 'st-nexus-tab',
        title: 'Nexus'
      });

      this.createSubComponents();
      
      Sonatype.view.serverTabPanel.add(this.nexusPanel);
      
      var htmlString = this.buildRecoveryText();
      
      if (htmlString != null){
    	this.loginFormConfig.items[2] = 
    	  {
    		xtype: 'panel',
    		id: 'recovery-panel',
    		style: 'padding-left: 70px',
    		html: htmlString
    	  };
      }
      
      this.loginFormConfig.buttons = [{ 
        id:'loginbutton',
        text:'Log In',
        tabIndex: 3, 
        formBind: true,
        scope: this,
        handler:function(){
          Sonatype.utils.doLogin( this.loginWindow,
            this.loginForm.find('name', 'username')[0].getValue(),
            this.loginForm.find('name', 'password')[0].getValue()); 
        } 
      }];
      
      this.loginFormConfig.keys = {
        key: Ext.EventObject.ENTER,
        fn: this.loginFormConfig.buttons[0].handler,
        scope: this
      };
      
      this.loginForm = new Ext.form.FormPanel(this.loginFormConfig);
      this.loginWindow = new Ext.Window({
        title:'Nexus Log In',
        animateTarget: 'login-link',
        closable: true,
        closeAction: 'hide',
        autoWidth: false,
        width: 300,
        autoHeight: true,
        modal:true,
        constrain: true,
        resizable: false,
        draggable: false,
        items: [this.loginForm]
      });
      
      this.loginWindow.on('show', function(){
        var panel = this.loginWindow.findById( 'recovery-panel' );
        if (panel && !panel.clickListenerAdded) {
          // these listeners only work if added after the window is created
          panel.body.on('click', Ext.emptyFn, null, {delegate:'a', preventDefault:true});
          panel.body.on('mousedown', this.recoverLogin, this, {delegate:'a'});
          panel.clickListenerAdded = true;
        }

        var field = this.loginForm.find('name', 'username')[0];
        if ( field.getRawValue() ) {
          field = this.loginForm.find('name', 'password')[0]
        }
        field.focus(true, 100);
      }, this);
      
      this.loginWindow.on('close', function(){
        this.loginForm.getForm().reset();
      }, this);
      
      this.loginWindow.on('hide', function(){
        this.loginForm.getForm().reset();
      }, this);
    },
    
    //Add/Replace Nexus left hand components
    createSubComponents : function() {
      var wasRendered = this.nexusPanel.rendered;
      
      if (wasRendered) {
        this.nexusPanel.getEl().mask('Updating...', 'loading-indicator');
        this.nexusPanel.items.each(function(item, i, len){
          this.remove(item, true);
        }, this.nexusPanel);
      }

      Sonatype.Events.fireEvent( 'nexusNavigationInit', this.nexusPanel );

      if (wasRendered) {
        this.nexusPanel.doLayout();
        this.nexusPanel.getEl().unmask();
        //this.nexusPanel.enable();
      }
      
    },
    
    addNexusNavigationItems: function( nexusPanel ) {      
      if(sp.checkPermission('nexus:index', sp.READ)){
        nexusPanel.add(
          {
            title: 'Artifact Search',
            id: 'st-nexus-search',
            items: [
              {
                xtype: 'trigger',
                id: 'quick-search--field',
                triggerClass: 'x-form-search-trigger',
                repoPanel: this,
                width: 140,
                listeners: {
                  'specialkey': {
                    fn: function(f, e){
                      if(e.getKey() == e.ENTER){
                        this.onTriggerClick();
                      }
                    }
                  }
                },
                onTriggerClick: function(a,b,c){
                  var v = this.getRawValue();
                  if ( v.length > 0 ) {
                    var panel = Sonatype.view.mainTabPanel.addOrShowTab(
                      'st-nexus-search-panel', Sonatype.repoServer.SearchPanel, { title: 'Search' } );
                    var searchType = 'quick';
                    if ( v.search(/^[0-9a-f]{40}$/) == 0 ) {
                      searchType = 'checksum';
                    }
                    else if ( v.search(/^[A-Z]/) == 0 ) {
                      searchType = 'classname';
                    }
                    panel.setSearchType( panel, searchType );
                    panel.searchField.setRawValue( this.getRawValue() );
                    panel.startSearch( panel );
                  }
                }
              },
              {
                title: 'Advanced Search',
                tabCode: Sonatype.repoServer.SearchPanel,
                tabId: 'st-nexus-search-panel',
                tabTitle: 'Search'
              }
            ]
          }
        );
      }
      
      //Views Group **************************************************
      nexusPanel.add( {
        title: 'Views/Repositories',
        id: 'st-nexus-views',
        items: [
          {
            enabled: sp.checkPermission( 'nexus:repostatus', sp.READ ),
            title: 'Repositories',
            tabId: 'st-repositories',
            tabCode: Sonatype.repoServer.RepositoryPanel,
            tabTitle: 'Repositories'
          },
          {
            enabled: sp.checkPermission( 'nexus:feeds', sp.READ ),
            title: 'System Feeds',
            tabId: 'feed-view-system_changes',
            tabCode: Sonatype.repoServer.FeedViewPanel
          },
          {
            enabled: sp.checkPermission( 'nexus:logs', sp.READ ) ||
              sp.checkPermission( 'nexus:configuration', sp.READ ),
            title: 'Logs and Config Files',
            tabId: 'view-logs',
            tabCode: Sonatype.repoServer.LogsViewPanel,
            tabTitle: 'Logs and Configs'
          }
        ]
      } );

      //Config Group **************************************************
      nexusPanel.add( {
        title: 'Enterprise',
        id: 'st-nexus-enterprise'
      } );

      //Config Group **************************************************
      nexusPanel.add( {
        title: 'Administration',
        id: 'st-nexus-config',
        items: [
          {
            enabled: sp.checkPermission('nexus:settings', sp.READ) &&
              ( sp.checkPermission('nexus:settings', sp.CREATE) ||
                sp.checkPermission('nexus:settings', sp.DELETE) ||
                sp.checkPermission('nexus:settings', sp.EDIT)),
            title: 'Server',
            tabId: 'nexus-config',
            tabCode: Sonatype.repoServer.ServerEditPanel,
            tabTitle: 'Nexus'
          },
          {
            enabled: sp.checkPermission('nexus:routes', sp.READ) &&
              ( sp.checkPermission('nexus:routes', sp.CREATE) ||
                sp.checkPermission('nexus:routes', sp.DELETE) ||
                sp.checkPermission('nexus:routes', sp.EDIT)),
            title: 'Routing',
            tabId: 'routes-config',
            tabCode: Sonatype.repoServer.RoutesEditPanel
          },
          {
            enabled: sp.checkPermission('nexus:tasks', sp.READ) &&
              ( sp.checkPermission('nexus:tasks', sp.CREATE) ||
                sp.checkPermission('nexus:tasks', sp.DELETE) ||
                sp.checkPermission('nexus:tasks', sp.EDIT)),
            title: 'Scheduled Tasks',
            tabId: 'schedules-config',
            tabCode: Sonatype.repoServer.SchedulesEditPanel
          },
          {
            enabled: sp.checkPermission('nexus:targets', sp.READ) &&
              ( sp.checkPermission('nexus:targets', sp.CREATE) ||
                sp.checkPermission('nexus:targets', sp.DELETE) ||
                sp.checkPermission('nexus:targets', sp.EDIT)),
            title: 'Repository Targets',
            tabId: 'config-repoTargets',
            tabCode: Sonatype.repoServer.RepoTargetEditPanel
          },
          {
            enabled: sp.checkPermission('nexus:logconfig', sp.READ) &&
              ( sp.checkPermission('nexus:logconfig', sp.CREATE) ||
                sp.checkPermission('nexus:logconfig', sp.DELETE) ||
                sp.checkPermission('nexus:logconfig', sp.EDIT)),
            title: 'Log',
            tabId: 'log-config',
            tabCode: Sonatype.repoServer.LogEditPanel
          }          
        ]
      } );

      //Security Group **************************************************
      nexusPanel.add({
        title: 'Security',
        id: 'st-nexus-security',
        items: [
          {
            enabled: Sonatype.user.curr.isLoggedIn && Sonatype.user.curr.loggedInUserSource == 'default' && 
              sp.checkPermission( 'nexus:userschangepw', sp.CREATE ),
            title: 'Change Password',
            handler: Sonatype.utils.changePassword
          },
          {
            enabled: sp.checkPermission( 'nexus:users', sp.READ ) && 
              ( sp.checkPermission('nexus:users', sp.CREATE) &&
                sp.checkPermission('nexus:users', sp.DELETE) &&
                sp.checkPermission('nexus:users', sp.EDIT)),
            title: 'Users',
            tabId: 'security-users',
            tabCode: Sonatype.repoServer.UserEditPanel
          },
          {
            enabled: sp.checkPermission('nexus:roles', sp.READ ) && 
              ( sp.checkPermission('nexus:roles', sp.CREATE) ||
                sp.checkPermission('nexus:roles', sp.DELETE) ||
                sp.checkPermission('nexus:roles', sp.EDIT)),
            title: 'Roles',
            tabId: 'security-roles',
            tabCode: Sonatype.repoServer.RoleEditPanel
          },
          {
            enabled: sp.checkPermission('nexus:privileges', sp.READ ) && 
              ( sp.checkPermission('nexus:privileges', sp.CREATE) ||
                sp.checkPermission('nexus:privileges', sp.DELETE) ||
                sp.checkPermission('nexus:privileges', sp.EDIT)),
            title: 'Privileges',
            tabId: 'security-privileges',
            tabCode: Sonatype.repoServer.PrivilegeEditPanel
          }
        ]
      } );

      nexusPanel.add( {
        title: 'Help',
        id: 'st-nexus-docs',
        collapsible: true,
        collapsed: true,
        items: [
          {
            title: 'About Nexus',
            tabId: 'AboutNexus',
            tabCode: Sonatype.repoServer.HelpAboutPanel
          },
          { title: 'Nexus Home',
            href: 'http://nexus.sonatype.org/' },
          { title: 'Getting Started',
            href: 'http://www.sonatype.com/book/reference/repository-manager.html' },
          { title: 'Nexus Wiki',
            href: 'https://docs.sonatype.com/display/NX/Home' },
          { title: 'Maven Book',
            href: 'http://www.sonatype.com/book/reference/public-book.html' },
          { title: 'Release Notes',
            href: 'http://nexus.sonatype.org/changes.html' },
          { title: 'Report Issue',
            href: 'http://issues.sonatype.org/secure/CreateIssue.jspa?pid=10001&issuetype=1' }
        ]
      } );
    },
    
    loginHandler : function(){
      if (Sonatype.user.curr.isLoggedIn) {
        //do logout
        Ext.Ajax.request({
          scope: this,
          method: 'GET',
          url: Sonatype.config.repos.urls.logout,
          callback: function(options, success, response){
            Sonatype.utils.clearCookie('JSESSIONID');
            Sonatype.utils.clearCookie('nxRememberMe');
            Sonatype.utils.loadNexusStatus();
          }
        });
        
      }
      else {
        this.loginForm.getForm().clearInvalid();
        var cp = Sonatype.state.CookieProvider;
        var username = cp.get('username', null);
        if ( username ) {
          this.loginForm.find( 'name', 'username' )[0].setValue( username );
        }
        this.loginWindow.show();
      }
    },
    
    resetMainTabPanel: function() {
        Sonatype.view.mainTabPanel.items.each(function(item, i, len){
          this.remove( item, true );
        }, Sonatype.view.mainTabPanel);
        Sonatype.view.mainTabPanel.activeTab = null;
        
        Sonatype.view.welcomeTab = new Ext.Panel({
          title: 'Welcome'
        });
        
        var welcomeMsg = '<p>Welcome to the <a href="http://nexus.sonatype.org" target="new">Sonatype Nexus Maven Repository Manager</a>.</p>' ;
        
        if( !Sonatype.user.curr.isLoggedIn ){
        	welcomeMsg += '</br>';
        	welcomeMsg += '<p>Administrators may login via the link on the top right.<p>';
        }
        
        var searchEnabled = sp.checkPermission('nexus:index', sp.READ);
        var browseEnabled = sp.checkPermission('nexus:repostatus', sp.READ );
        if (searchEnabled || browseEnabled){
        	welcomeMsg += '</br>';
        	if (searchEnabled && browseEnabled){
        		welcomeMsg += '<p>You may browse and search the repositories using the options on the left.</p>'
        	}
        	else if (searchEnabled && !browseEnabled){
        		welcomeMsg += '<p>You may search the repositories using the options on the left.</p>'
        	}
        	else if (!searchEnabled && browseEnabled){
        		welcomeMsg += '<p>You may browse the repositories using the options on the left.</p>'
        	}
        }
        
        Sonatype.view.welcomeTab.html = '<div class="little-padding">' + welcomeMsg + '</div>';
        Sonatype.view.mainTabPanel.add(Sonatype.view.welcomeTab);
        Sonatype.view.mainTabPanel.setActiveTab(Sonatype.view.welcomeTab);
    },

    recoverLogin : function(e, target){
      e.stopEvent();
      if (this.loginWindow.isVisible()) {
    	this.loginWindow.hide();
      }
      
      var action = target.id;
      if (action == 'recover-username') {
    	  Sonatype.utils.recoverUsername();
      }
      else if (action == 'recover-password') {
      	Sonatype.utils.recoverPassword();
      }
    }
     
  };
}();


})();
