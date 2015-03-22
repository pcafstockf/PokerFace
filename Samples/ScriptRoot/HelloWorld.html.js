/*
 * Simple Hello World JavaScript endpoint to demonstrate and document PokerFace's scripting abilities.
 * This script examines the 'Accept-Language' header and responds with a classic 'Hello World' in English, French, or Spanish.
 * This script also assumes that the http://momentjs.com library has been pre-loaded by the PokerFace configuration file.
 * 
 * For documentation of the methods please see EndpointTemplate.js
 */
(function() {
	var localeData = {
		en: { },
		fr: { },
		es: { }
	};
	return {
		apiVersion: 1,
		
		setup: function(path, config, logger, callback) {
			// Populate any muttable locale data.
			localeData.en.formatter = moment();
			localeData.en.formatter.locale('en');
			localeData.en.message = config.getString('en');
			localeData.fr.formatter = moment();
			localeData.fr.message = config.getString('fr');
			localeData.fr.formatter.locale('fr');
			localeData.es.formatter = moment();
			localeData.es.formatter.locale('es');
			localeData.es.message = config.getString('es');

			var Thread = Java.type('java.lang.Thread');
			new Thread(function() {
				logger.info(path + ' setup successfully.');
				callback.setupComplete();
			}).start();
		},
		
		inspectRequest: function(request, context) {
			return this;
		},

		generateResponse: function(request, context) {
			var response = {
				statusCode: 200,
				mimeType: 'text/html',
				charset: 'utf-8',
				content: function(request, context) {
					var helper = context.getAttribute('pokerface.scriptHelper');
					var locale = 'en';
					var acceptables = helper.getAcceptableLocales();
					for (var idx=0; idx<acceptables.length; idx++)
						if (acceptables[idx] in localeData) {
							locale = acceptables[idx];
							break;
						}
					// It's okay to read immutable data or invoke immutable functions / methods.
					return '<html><head><title>Welcome</title></head><body>' + localeData[locale].message + '<br/>' + localeData[locale].formatter.format('MMMM Do YYYY, h:mm:ss a') + '</body></html>';
				}
			};
			return response;
		},
		
		teardown: function() {
		}
	};
})();
