// AJS equivalent of document.ready
AJS.toInit(function($) {

  function sortOptions(element) {

    var options = element.find( 'option' );
    var selected = element.val();
    
    options.sort( function( a, b ) {
        if (a.text > b.text) return 1;
        if (a.text < b.text) return -1;
        return 0
    })
    
    element.empty().append( options );
    element.val( selected );

  }

  // only sort once per page-load
  var sorted = { dialogReady: false, inlineEditStarted: false };
  JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, context, reason) {

    var context = AJS.$(context);
    var selectBox = context.find('.mite-project-selector-select');

    // if necessary, alphabetize options instead of providing a random list
    switch (reason) {
      case 'dialogReady':
        sortOptions(selectBox);
        sorted[reason] = true;
        break;
      case 'inlineEditStarted':
        sortOptions(selectBox);
        sorted[reason] = true;
        break;
    }

    // use JIRA's autocomplete renderer instead of using a simple select-element
    new AJS.SingleSelect({
      element: selectBox,
      itemAttrDisplayed: 'label',
      errorMessage: AJS.params.multiselectGenericError
    });

  });

});