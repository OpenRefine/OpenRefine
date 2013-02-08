var RdfDataTableView = {};

RdfDataTableView.previewExprsOnVisibleRows = function(column, title, expression, isRowNumberCell, onDone){
	return RdfDataTableView.previewOnVisibleRows(true,column,title, expression,isRowNumberCell,'',onDone);
};
RdfDataTableView.previewUrisOnVisibleRows = function(column, title, expression, isRowNumberCell, baseUri, onDone) {
	return RdfDataTableView.previewOnVisibleRows(false,column,title, expression, isRowNumberCell, baseUri, onDone);
};

RdfDataTableView.previewOnVisibleRows = function(isLiteral,column, title, expression, isRowNumberCell, baseUri, onDone) {
	var o = DataTableView.sampleVisibleRows(column);
    
    var self = this;
    
  	function f () {}
    	
   	$.extend(f.prototype,ExpressionPreviewDialog.prototype);
    	
    	/*f = function(){
    		
    	};*/
    
   	f.prototype.generateWidgetHtmlOnlyGrel = function() {
	    var html = DOM.loadHTML("core", "scripts/dialogs/expression-preview-dialog.html");
	    
	    var languageOptions = [];
	    var prefix = 'grel';
           var info = theProject.scripting[prefix];
	        languageOptions.push('<option value="' + prefix + '">' + info.name + '</option>');
	    
	    return html.replace("$LANGUAGE_OPTIONS$", languageOptions.join(""));
	};
	
   	f.prototype.create= function(isLiteral,title, columnName, rowIndices, values, expression, baseUri,isRowNumberCell, onDone){
   		var uriPreviewWidget = RdfDataTableView.getUriPreviewWidget(isLiteral,isRowNumberCell,baseUri);
    	f._onDone = onDone;
        var self = this;
        var frame = DialogSystem.createDialog();
        frame.width("700px");
        
        var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
        var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
        var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
        var html = $(self.generateWidgetHtmlOnlyGrel()).appendTo(body);
        
        f._elmts = DOM.bind(html);
        
        $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        	DialogSystem.dismissUntil(f._level - 1);
            f._onDone(f._previewWidget.getExpression(true));
        }).appendTo(footer);
        
        $('<button></button>').addClass('button').text("Cancel").click(function() {
        	DialogSystem.dismissUntil(f._level - 1);
        }).appendTo(footer);
        
        f._level = DialogSystem.showDialog(frame);
            
//        	this._previewWidget = new ExpressionPreviewDialog.Widget(this._elmts,cellIndex,rowIndices,values,expression);
        f._previewWidget = function(){
        	
        	uriPreviewWidget._elmts = f._elmts;
        	uriPreviewWidget._columnName = columnName;
        	uriPreviewWidget._rowIndices = rowIndices;
        	uriPreviewWidget._values = values;
            
        	uriPreviewWidget._results = null;
        	uriPreviewWidget._timerID = null;
            
        	if (!(expression)) {
        		uriPreviewWidget.expression = 'value';
        	}else{
        		uriPreviewWidget.expression = expression;
        	}
        	
        	uriPreviewWidget._tabContentWidth = f._elmts.expressionPreviewPreviewContainer.width() + "px";
        	
        	$("#expression-preview-tabs").tabs();
        	$("#expression-preview-tabs-history").css("display", "");
        	$("#expression-preview-tabs-help").css("display", "");
            
        	uriPreviewWidget._elmts.expressionPreviewLanguageSelect[0].value = "grel";
        	uriPreviewWidget._elmts.expressionPreviewLanguageSelect.bind("change", function() {
        		$.cookie("scripting.lang", this.value);
        		self.update();
        	});
                
        	uriPreviewWidget._elmts.expressionPreviewTextarea
        	.attr("value", uriPreviewWidget.expression)
            .keyup(function(){
            	uriPreviewWidget._scheduleUpdate();
            })
            .select()
            .focus();
            
        	uriPreviewWidget.update();
        	uriPreviewWidget._renderExpressionHistoryTab();
        	uriPreviewWidget._renderHelpTab();
            
        	return uriPreviewWidget;
        }();
    };
        
   	new f().create(isLiteral,title,column.columnName,o.rowIndices,o.values,expression,baseUri,isRowNumberCell,onDone);	
};




RdfDataTableView.getUriPreviewWidget = function(isLiteral,isRowNumberCell,baseUri){
	function widgetDescendant() {}
	$.extend(widgetDescendant.prototype,ExpressionPreviewDialog.Widget.prototype);

	widgetDescendant.prototype.update = function(){
		var self = this;
		var expression = this.expression = $.trim(this._elmts.expressionPreviewTextarea[0].value);
		var params = {
				project: theProject.id,
				expression: expression,
				isUri:isLiteral?"0":"1",
				columnName: isRowNumberCell?"":this._columnName,
				baseUri:baseUri
		};
		this._prepareUpdate(params);
    
		var cmdUrl = "command/rdf-extension/preview-rdf-expression?" ;
		$.post(
				cmdUrl + $.param(params), 
				{
					rowIndices: JSON.stringify(this._rowIndices) 
				},
				function(data) {
					if (data.code != "error") {
						self._results = data.results;
					} else {
						self._results = null;
					}
					self._renderPreview(expression, isRowNumberCell, data);
				},	
				"json"
		);
	};

	widgetDescendant.prototype._renderPreview = function(expression, isRowNumberCell, data) {
		var container = this._elmts.expressionPreviewPreviewContainer.empty();
		var table = $('<table width="100%"></table>').appendTo(container)[0];
    
    
		tr = table.insertRow(0);
		$(tr.insertCell(0)).addClass("expression-preview-heading").text("row");
		$(tr.insertCell(1)).addClass("expression-preview-heading").text(isRowNumberCell?"row.index":"value");
		$(tr.insertCell(2)).addClass("expression-preview-heading").text(expression);
		if(!isLiteral){
			$(tr.insertCell(3)).addClass("expression-preview-heading").text("resolved against the base URI");
		}
    
		var renderValue = function(td, v) {
			if (v !== null && v !== undefined) {
				if ($.isPlainObject(v)) {
					$('<span></span>').addClass("expression-preview-special-value").text("Error: " + v.message).appendTo(td);
				} else {
					td.text(v);
				}
			} else {
				$('<span>null</span>').addClass("expression-preview-special-value").appendTo(td);
			}
		};
    
		if (this._results !== null) {
			this._elmts.expressionPreviewParsingStatus.empty().removeClass("error").text("No syntax error.");
		} else {
			var message = (data.type == "parser") ? data.message : "Internal error";
			this._elmts.expressionPreviewParsingStatus.empty().addClass("error").text(message);
		}
    
		for (var i = 0; i < this._values.length; i++) {
			var tr = table.insertRow(table.rows.length);
        
			$(tr.insertCell(0)).attr("width", "1%").html((this._rowIndices[i] + 1) + ".");
        
			renderValue($(tr.insertCell(1)).addClass("expression-preview-value"), isRowNumberCell?this._rowIndices[i]:this._values[i]);
        
			var tdValue = $(tr.insertCell(2)).addClass("expression-preview-value");
			if (this._results !== null) {
				var v = this._results[i];
				renderValue(tdValue, v);
			}
			if(!isLiteral){
				var absolutTtdValue = $(tr.insertCell(3)).addClass("expression-preview-value");
				if (this._results !== null) {
					var v = data.absolutes[i]==null?this._results[i]:data.absolutes[i];
					renderValue(absolutTtdValue, v);
				}
			}
		}
	
	};

	var uriPreviewWidget = new widgetDescendant();
	return uriPreviewWidget;
};
