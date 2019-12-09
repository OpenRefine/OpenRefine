// in this directory, run 
// npm i puppeteer
// npm i puppeteer-core

//then, run node test.js to run the test

// note: must be running ./refine first

// other useful examples: https://developers.google.com/web/tools/puppeteer/examples

const puppeteer = require("puppeteer");
// const chalk = require ("chalk");

//colors for console logs
// const error = chalk.bold.red;
// const success = chalk.keyword("green");

// const username = 'test';
// const password = 'test';

// const readline = require('readline');
// const rl = readline.createInterface({
//   input: process.stdin,
//   output: process.stdout
// });



  (async () => {
    try {
      var browser = await puppeteer.launch({
        headless: false,
        devtools: true
      });
      var page = await browser.newPage();
      // For future tests, issues and preview can be easily accessed with this size
      await page.setViewport({
        width: 900,
        height: 1000,
      });
  
      /******************************************
       *        OPENREFINE REACH TESTING        *
       * (can we reach a project on openrefine) *
       ******************************************/ 

      // UNIT TEST: OpenRefine homepage can be reached
      await page.goto(`http://127.0.0.1:3333/`);
      var content = await page.content();
      if (content != null){
        console.log('TEST PASSED: OpenRefine homepage is reached')
      } else {
        console.log('TEST FAILED: OpenRefine homepage is reached')
      }

      // UNIT TEST: The first project can be reached
      var projectURL = await page.evaluate(() => {
        let elements = $('.action-area-tab').toArray();
        $(elements[1]).click(); 
        //console.log("asdfa");

        let openProj = document.querySelectorAll('.project-name');

        return openProj[0].href;
      });

      //await console.log(projectURL);

      await page.goto(`${projectURL}`);
      var content = await page.content();
      await page.waitFor(1000);
      
      if (content != null){
        console.log('TEST PASSED: Project page is reached')
      } else {
        console.log('TEST FAILED: Project page is reached')
      }

      /*************************************
       *      EXTENSION REACH TESTING      *
       * (can we reach our extension code) *
       *************************************/ 

      // UNIT TEST: SNAC extension dropdown button exists on main page
      var extensionDropDownExistTest = false;

      const extensionButton = await page.$('#body > #right-panel > #tool-panel > #extension-bar > #extension-bar-menu-container');
      const listOfExtensions = await extensionButton.$$('a');
      for (const button of listOfExtensions){
          var text = await page.evaluate(button => button.innerText, button);
          if(text == 'SNAC'){
            button.click();
            extensionDropDownExistTest = true;
            break;
          }
      }
      if(extensionDropDownExistTest){
        console.log("TEST PASSED: SNAC Extension button exists.");
      } else {
        console.log("TEST FAILED: SNAC Extension button exists.");
      }
      await page.waitFor(1000);
      

      // UNIT TEST: 'Edit SNAC schema' will take you to the SNAC extension frontend
      var editSchmeaOptionExistTest = false;

      const menuHandler = await page.$$('.menu-container > .menu-item');
      for (const menuOption of menuHandler){
        var text = await page.evaluate(option => option.innerText, menuOption);
        if(text == 'Edit SNAC schema'){
          // Access the snac chema edit tabs
          menuOption.click();
          editSchmeaOptionExistTest = true;
          break;
        }
      }
      if(editSchmeaOptionExistTest){
        console.log("TEST PASSED: 'Edit SNAC schema' option exists in SNAC extension dropdown.");
      } else {
        console.log("TEST FAILED: 'Edit SNAC schema' option exists in SNAC extension dropdown.");
      }
      await page.waitFor(1000);

      /********************** 
       * ISSUES TAB TESTING *
       **********************/ 

      // UNIT TEST: Issues tab exists
      var issuesExistTest = false;
    
      const tabHandlers = await page.$$('.main-view-panel-tabs-snac');
      for (const tab of tabHandlers){
        var text = await page.evaluate(tab => tab.innerText, tab);
        if (text == 'Issues '){
          tab.click();
          issuesExistTest = true;
          break;
        }
      }
      if(issuesExistTest){
        console.log("TEST PASSED: Issues tab appears after clicking 'Edit SNAC schema'.");
      } else {
        console.log("TEST FAILED: Issues tab appears after clicking Edit SNAC schema.");
      }
      await page.waitFor(1000);

      // UNIT TEST: Number of issues are 4 (for the number of missing fields)
      const issuesResults = await page.$$('#snac-issues-panel > table > tbody > .wb-warning');
      if(issuesResults.length == 4){
        console.log("TEST PASSED: 4 errors appear on the issues tab when no edits were made to schema.");
      } else {
        console.log("TEST FAILED: 4 errors appear on the issues tab when no edits were made to schema.");
      }
      await page.waitFor(1000);

      // const html = await page.evaluate(button => button.innerHTML, extensionButtons[0]);
      // console.log(html);
      // const extensionButtons = await extensionElement.$('')
      
      //   await page.evaluate(async () => {

      //     let names = [];


      //     function SearchElementForSelector(el, s) {
      //       while (el && (el.tagName && !el.matches(s))) {
      //         el = el.parentNode;
      //       }

      //       if (el && el.tagName && el.matches(s)) {
      //         return el;
      //       }
      //       return null;
      //     }

      //     function GetParams(url) {
      //       let queries = {};
      //       let parts = url.split('?');
      //       if (parts.length == 2) {
      //         parts[1].split('&').forEach((pair)=>{
      //           let params = pair.split('=');
      //           queries[params[0]] = params[1];
      //         });
      //       }
      //       return queries;
      //     }
      //     function CheckAnchorQueries(anchor) {
      //       if (anchor && anchor.href) {
      //         let eName = GetParams(anchor.href)['ya-track'];
      //         if (eName) {
      //           return eName;
      //         }
      //       }
      //       return false;
      //     }
      //     let SelectorTracking = {};

      //     var events = document.querySelectorAll('button, a, input');

      //     events.forEach(function(element){
      //       try {
      //         let type = null;
      //         let trackDetails = null;
      //         let srcEl = null;

      //         for (const selector in SelectorTracking) {
      //           if (!element.matches(selector)) continue;
      //           trackDetails = SelectorTracking[selector];
      //         }

      //         if (!trackDetails) {
      //           let potentialYaTrackedEl = SearchElementForSelector(element, '[data-ya-track]');
      //           if (potentialYaTrackedEl) {
      //             srcEl = potentialYaTrackedEl;
      //             trackDetails = (potentialYaTrackedEl.dataset ? potentialYaTrackedEl.dataset.yaTrack : potentialYaTrackedEl.getAttribute('data-ya-track'));
      //           }
      //         }

      //         let preventDefaultEvent = SearchElementForSelector(element, '[data-ya-prevent-default]');

      //         let vectorMap = SearchElementForSelector(element, '.VectorMap-link');

      //         if (!preventDefaultEvent && !trackDetails && !vectorMap) {
      //           let anchor = SearchElementForSelector(element, 'a');
      //           if (anchor) {
      //             srcEl = anchor;
      //             let anchorQuery = CheckAnchorQueries(anchor);
      //             if (anchorQuery) trackDetails = anchorQuery;
      //             if (!anchorQuery && !trackDetails) {
      //               type = 'link';
      //             }
      //           }
      //         }

      //         if (!preventDefaultEvent && !trackDetails && !type && !vectorMap) {
      //           let button = SearchElementForSelector(element, 'button');
      //           if (button) {
      //             srcEl = button;
      //             type = 'button';
      //           }
      //         }

      //         if (!preventDefaultEvent && !trackDetails && !type && !vectorMap) {
      //           let input = SearchElementForSelector(element, 'input');
      //           if (input && input.type != 'hidden') {
      //             srcEl = input;
      //             type = 'input';
      //           }
      //         }

      //         let dataYaTrack = type || trackDetails;
      //         let name = null;

      //         if(dataYaTrack)
      //         {
      //           let scopeAncestors = [];
      //           while (element && element.tagName) {
      //             if (element.matches('[data-ya-scope]')) {
      //               scopeAncestors.push(element);
      //             }
      //             element = element.parentNode;
      //           }

      //           let tags = [srcEl].concat(scopeAncestors);
      //             for (const [hierarchyIdx, hierarchyElement] of tags.entries()) {
      //               let tagVal = (hierarchyIdx == 0) ? dataYaTrack : (hierarchyElement.dataset ? hierarchyElement.dataset.yaScope : hierarchyElement.getAttribute('data-ya-scope'))
      //               if (tagVal.indexOf('#') > -1) {
      //                 let attributeName = hierarchyIdx == 0 ? 'data-ya-track': 'data-ya-scope';
      //                 let ancestor = (hierarchyIdx + 1 < tags.length) ? tags[hierarchyIdx + 1]: document;
      //                 let siblings = Array.from(ancestor.querySelectorAll(`[${attributeName}='${tagVal}']`));
      //                 for (const [siblingIdx, sibling] of siblings.entries()) {
      //                   if (hierarchyElement == sibling) {
      //                     tagVal = tagVal.replace('#', siblingIdx + 1);
      //                     break;
      //                   }
      //                 }
      //               }
      //               tags[hierarchyIdx] = tagVal;
      //             }
      //             let name = tags.reverse().join('_');
      //             names.push(name);
      //         }
      //       } catch (err){
      //         console.log(err);
      //       }
      //       });
      //       names.forEach(function(name){
      //         var url = window.CalcEventNameForElement(name, pagesReferrer, pageurl);
      //         url.then(data => {
      //           const px = document.createElement("img");
      //           px.src = data;
      //           px.style.width = '0';
      //           px.style.height = '0';
      //           px.style.position = 'absolute';
      //           px.alt = '';
      //           document.body.appendChild(px);
      //           return px;
      //         });
      //       });

      //   });

      await page.waitFor(10000);
      await browser.close();
      console.log("Browser Closed");
    } catch (err) {
        console.log(err);
        await browser.close();
        console.log("Error: Browser Closed");
    }
  })();
 