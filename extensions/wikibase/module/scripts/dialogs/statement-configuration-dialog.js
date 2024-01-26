
class PropertyStrategy {
  id() {
    return 'property';
  }

  initialState() {
    return {
      'type': this.id()
    };
  }

  renderUI(parentElem, currentState, onChange) {
     $('<p></p>').text($.i18n('wikibase-statement-settings/no-strategy-options'))
        .appendTo(parentElem);
  }
};

class SnakStrategy {
  id() {
    return 'snak';
  }

  initialState() {
    return {
      'type': this.id(),
      'valueMatcher': {
        'type': 'lax'
      }
    };
  }

  renderUI(parentElem, currentState, onChange) {
     var self = this;
     const checkbox = $('<input></input>')
        .attr('name', 'laxValueMatching')
        .attr('id', 'laxValueMatchingId')
        .attr('type', 'checkbox')
        .appendTo(parentElem);
     $('<label></label>').text($.i18n('wikibase-statement-settings/lax-value-matching'))
        .attr('for', 'laxValueMatchingId')
        .appendTo(parentElem);
     checkbox.attr('checked', (!currentState.valueMatcher) || currentState.valueMatcher.type == 'lax');
     checkbox.on('change',function() {
        const checked = $(this).prop('checked');
        onChange({'type': self.id(), 'valueMatcher': {'type': checked ? 'lax' : 'strict' }});
     });
  }
};

class QualifiersStrategy {
  id() {
    return 'qualifiers';
  }

  initialState() {
    return {
      'type': this.id(),
      'valueMatcher': {
        'type': 'lax'
      },
      'pids': []
    };
  }

  renderUI(parentElem, currentState, onChange) {
     var self = this;
     const checkbox = $('<input></input>')
        .attr('name', 'laxValueMatching')
        .attr('id', 'laxValueMatchingId')
        .attr('type', 'checkbox')
        .appendTo(parentElem);
     $('<label></label>').text($.i18n('wikibase-statement-settings/lax-value-matching'))
        .attr('for', 'laxValueMatching')
        .appendTo(parentElem);
     checkbox.attr('checked', (!currentState.valueMatcher) || currentState.valueMatcher.type == 'lax');

     parentElem.append($('<br />'));
     $('<label></label>').text($.i18n('wikibase-statement-settings/discriminating-qualifiers-label'))
        .attr('for', 'discriminatingQualifiersId')
        .appendTo(parentElem);
     let pidInput = $('<input></input>')
        .attr('type', 'text')
        .attr('style','margin: 0.5em 0 0 0.5em')
        .attr('name', 'discriminatingQualifiers')
        .attr('id', 'discriminatingQualifiersId')
        .val(currentState.pids ? currentState.pids.join(',') : '')
        .appendTo(parentElem);
     parentElem.append($('<br />'));
     $('<p></p>').text($.i18n('wikibase-statement-settings/discriminating-qualifiers-description'))
        .attr('style','margin-top: 0.5em')
        .appendTo(parentElem);

     let pidRegex = /^P[1-9]\d*/;
     let onOptionsChange = function() {
        let lax = checkbox.prop('checked');
        let pids = pidInput.val().split(',')
                .map(s => s.trim())
                .filter(s => pidRegex.test(s));
        onChange({
          'type': self.id(),
          'valueMatcher': {
            'type': lax ? 'lax' : 'strict'
          },
          'pids': pids
        });
     };
     checkbox.on('change',onOptionsChange);
     pidInput.on('input', onOptionsChange);

  }
};

var StatementConfigurationDialog = {};

// The available statement editing modes
StatementConfigurationDialog.possibleModes = ['add_or_merge', 'add', 'delete'];
StatementConfigurationDialog.defaultMode = 'add_or_merge';

// The available statement merging strategies
StatementConfigurationDialog.possibleStrategies = new Map();
StatementConfigurationDialog.possibleStrategies.set('qualifiers', new QualifiersStrategy());
StatementConfigurationDialog.possibleStrategies.set('snak', new SnakStrategy());
StatementConfigurationDialog.possibleStrategies.set('property', new PropertyStrategy());
StatementConfigurationDialog.defaultStrategy = {'type':'snak','valueMatcher':{'type':'lax'}};

StatementConfigurationDialog.launch = function(statement) {
  var self = this;

  this.frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/statement-configuration-dialog.html"));
  var frame = this.frame;
  this._elmts = DOM.bind(frame);

  this._level = DialogSystem.showDialog(frame);
  this._elmts.dialogHeader.text($.i18n('wikibase-statement-settings/dialog-header'));
  this._elmts.closeButton.text($.i18n('wikibase-schema/close-button'));
  this._elmts.modeText.text($.i18n('wikibase-statement-settings/mode-label'));
  this._elmts.strategyText.text($.i18n('wikibase-statement-settings/strategy-label'));
  this._elmts.strategyOptionsText.text($.i18n('wikibase-statement-settings/strategy-options'));

  this._configHasChanged = false;

  for (const mode of self.possibleModes) {
    $('<option></option>')
        .attr('value', mode)
        .text($.i18n('wikibase-statement-settings/mode-options/'+mode+'/label'))
        .appendTo(this._elmts.modeInput);
  }
  const currentMode = statement.data('jsonMode');
  self._elmts.modeInput.val(currentMode);
  self._elmts.modeDescription.text($.i18n('wikibase-statement-settings/mode-options/'+currentMode+'/description'));

  for (const strategy of self.possibleStrategies.keys()) {
    $('<option></option>')
        .attr('value', strategy)
        .text($.i18n('wikibase-statement-settings/strategy-options/'+strategy+'/label'))
        .appendTo(this._elmts.strategyInput);
  }
  var currentStrategy = JSON.parse(statement.data('jsonMergingStrategy'));
  const currentStrategyType = currentStrategy['type'];
  self._elmts.strategyInput.val(currentStrategyType);
  self._elmts.strategyDescription.text($.i18n('wikibase-statement-settings/strategy-options/'+currentStrategyType+'/description'));

  // render initial strategy options
  const strategy = self.possibleStrategies.get(currentStrategyType);
  strategy.renderUI(this._elmts.strategyOptions, currentStrategy, function(strategyOptions) {
    statement.data('jsonMergingStrategy', JSON.stringify(strategyOptions));
    self._configHasChanged = true;
  });

  this._elmts.modeInput.on('change',function(e) {
    const selected = $(this).val();
    statement.data('jsonMode', selected);
    self._elmts.modeDescription.text($.i18n('wikibase-statement-settings/mode-options/'+selected+'/description'));
    self._configHasChanged = true;
  });
  this._elmts.strategyInput.on('change',function(e) {
    const selected = $(this).val();
    const strategy = StatementConfigurationDialog.possibleStrategies.get(selected);
    const initialOptions = strategy.initialState();
    statement.data('jsonMergingStrategy', JSON.stringify(initialOptions));
    self._elmts.strategyDescription.text($.i18n('wikibase-statement-settings/strategy-options/'+selected+'/description'));
    self._elmts.strategyOptions.empty();
    strategy.renderUI(self._elmts.strategyOptions, initialOptions, function(strategyOptions) {
      statement.data('jsonMergingStrategy', JSON.stringify(strategyOptions));
    });
    self._configHasChanged = true;
  });

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  this._elmts.closeButton.on('click',function() {
    dismiss();
    if (self._configHasChanged) {
       SchemaAlignment._updateStatementFromMergingStrategy(statement);
       SchemaAlignment._hasChanged();
    }
  });

};

