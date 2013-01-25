RdfSchemaAlignmentDialog.UILink = function(dialog, link, table, options, parentUINode) {
	this._dialog = dialog;
    this._link = link;
    this._options = options;
    this._parentUINode = parentUINode;
    
    // Make sure target node is there
    this._link.target = this._link.target || { nodeType: "cell-as-literal" }
    
    this._tr = table.insertRow(table.rows.length);
    this._tdMain = this._tr.insertCell(0);
    this._tdToggle = this._tr.insertCell(1);
    this._tdDetails = this._tr.insertCell(2);
    
    $(this._tdMain).addClass("schema-alignment-link-main").attr("width", "250").addClass("padded");
    $(this._tdToggle).addClass("schema-alignment-link-toggle").attr("width", "1%").addClass("padded");
    $(this._tdDetails).addClass("schema-alignment-link-details").attr("width", "90%");
    
    this._collapsedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("padded").html("...");
    this._expandedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("schema-alignment-detail-container");
    var self = this;
    var show = function() {
        if (self._options.expanded) {
            self._collapsedDetailDiv.hide();
            self._expandedDetailDiv.show();
        } else {
            self._collapsedDetailDiv.show();
            self._expandedDetailDiv.hide();
        }
    };
    show();
    
    $(this._tdToggle).html("&nbsp;");
    $('<img />')
        .attr("src", this._options.expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(this._tdToggle)
        .click(function() {
            self._options.expanded = !self._options.expanded;
            
            $(this).attr("src", self._options.expanded ? "images/expanded.png" : "images/collapsed.png");
            
            show();
        });
    
    this._renderMain();
    this._renderDetails();
};


RdfSchemaAlignmentDialog.UILink.prototype._renderMain = function() {
    $(this._tdMain).empty();
    var l = this._getTypeName(this._link);
    var label =  l || "property?";
    
    var self = this;
    
    $('<img />')
        .attr("title", "remove property")
        .attr("src", "images/close.png")
        .css("cursor", "pointer")
        .prependTo(this._tdMain)
        .click(function() {
            window.setTimeout(function() {
                self._parentUINode.removeLink(self);
                self._tr.parentNode.removeChild(self._tr);
                //self._dialog.preview();
            }, 100);
        });
    
    var a = $('<a href="javascript:{}"></a>')
        .addClass("schema-alignment-link-tag")
        .html(RdfSchemaAlignmentDialog.UINode._shortenResource(label))
        .appendTo(this._tdMain)
        .click(function(evt) {
            self._startEditProperty(this);
        });
        
    $('<img />').attr("src", "images/arrow-start.png").prependTo(a);
    $('<img />').attr("src", "images/arrow-end.png").appendTo(a);
};

RdfSchemaAlignmentDialog.UILink.prototype._renderDetails = function() {
    if (this._targetUI) {
        this._targetUI.dispose();
    }
    if (this._tableDetails) {
        this._tableDetails.remove();
    }
    
    this._tableDetails = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._expandedDetailDiv);
    this._targetUI = new RdfSchemaAlignmentDialog.UINode(
        this._dialog,
        this._link.target, 
        this._tableDetails[0], 
        { expanded: "links" in this._link.target && this._link.target.links.length > 0 });
};

RdfSchemaAlignmentDialog.UILink.prototype._startEditProperty = function(elmt) {
	var self = this;
	new RdfSchemaAlignmentDialog.RdfResourceDialog(elmt,'property',theProject.id,self._dialog,self._dialog._prefixesManager,function(obj){
			self._link.uri = obj.id;
			self._link.curie = obj.name;
			self._renderMain();
	}
	);
};

RdfSchemaAlignmentDialog.UILink.prototype._getTypeName = function(t){
	if(t.curie !== undefined && t.curie !==''){
		return t.curie;
	}else{
		return t.uri;
	}
};

RdfSchemaAlignmentDialog.UILink.prototype.getJSON = function() {
    if ("uri" in this._link && this._link.uri !== null &&
        "target" in this._link && this._link.target !== null) {
        
        var targetJSON = this._targetUI.getJSON();
        if (targetJSON !== null) {
            return {
                uri: this._link.uri,
                curie:this._link.curie,
                target: targetJSON
            };
        }
    }
    return null;
};