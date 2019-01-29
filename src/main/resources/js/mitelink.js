// AJS equivalent of document.ready
AJS.toInit(function($) {

  // Sorts the options of a select element
  // by value, ascending (alphabetically)
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

  // We only sort an element once per page-load
  // As there can be multiple elements (see below),
  // we need to keep track of state
  var sorted = { dialogReady: false, inlineEditStarted: false };

  // Event-Handler that gets called whenever
  //
  // - We open a 'create new issue'-dialog
  // - We start to inline-edit an existing issue
  JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, context, reason) {

    // The context is basically a specific part of the page, i.e the issue dialog
    var context = AJS.$(context);

    // .. which we can then search for field
    var selectBox = context.find('.mite-project-selector-select');

    // If necessary, alphabetize options instead of providing a random list
    // We can have multiple fields on one page (i.e an active inline-edit and an overlaying create-issue dialog),
    // so it may be necessary to sort more than once
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

    // Finally, use JIRA's single-select autocomplete-renderer
    new AJS.SingleSelect({
      element: selectBox,
      itemAttrDisplayed: 'label',
      errorMessage: AJS.params.multiselectGenericError
    });

  });

});