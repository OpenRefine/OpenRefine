// in this directory, run 
// npm i puppeteer
// npm i puppeteer-core

//then, run node test.js to run the test

// note: must be running ./refine first

/* 
cd extensions/snac/tests/src/org/openrefine/snac/testing/tests
or
node extensions/snac/tests/src/org/openrefine/snac/testing/tests/test.js
*/
// other useful examples: https://developers.google.com/web/tools/puppeteer/examples

const puppeteer = require("puppeteer");

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
        let elements = $('.action-area-tab').toArray()[1];
        $(elements).click(); 
        return document.querySelectorAll('.project-name')[0].href;
      });
      await page.goto(`${projectURL}`);

      var content = await page.content();
      await page.waitFor(1000);
      
      if (content != null){
        console.log('TEST PASSED: Project page is reached')
      } else {
        console.log('TEST FAILED: Project page is reached')
      }

      
      // var text = await page.evaluate(button => button.innerText, uploadButton);
      // console.log(text);
      // uploadButton.click();
      // let upload = document.getElementsByClassName('menu-item')[2];
      // $(upload).click();
      // console.log(upload);

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
      
      // UNIT TEST: API Key Upload Option Exists
      var uploadAPIKeyOptionExistTest = false;
      var menuHandler = await page.$$('.menu-container > .menu-item');
      for (const menuOption of menuHandler){
        var text = await page.evaluate(option => option.innerText, menuOption);
        if(text == 'Upload edits to SNAC'){
          menuOption.click();
          uploadAPIKeyOptionExistTest = true;
          break;
        }
      }
      if(uploadAPIKeyOptionExistTest){
        console.log("TEST PASSED: 'Upload edits to SNAC' option exists in SNAC extension dropdown.");
      } else {
        console.log("TEST FAILED: 'Upload edits to SNAC' option exists in SNAC extension dropdown.");
      }
      await page.waitFor(1000);

      // UNIT TEST: API Key Upload Verification Appears
      var uploadAPIKeyVerificationTest = false;
      const uploadAPIHandler = await page.$('.snac-upload-management-area > .snac-upload-form > table > tbody > tr > td');
      var text = await page.evaluate(option => option.innerText, uploadAPIHandler);
      if (text == 'Your API key is:'){
        console.log('TEST PASSED: API Key Upload Verification');
      } else {
        console.log('TEST PASSED: API Key Upload Verification');
      }

      // UNIT TEST: Development Site Upload Verification
      var devProdHandler = await page.evaluate(selected => {
        let devOrProd = $('.siteInput').get();
        if(devOrProd[0].checked){
          selected = document.getElementsByClassName('siteInput')[0].value;
        } else if(devOrProd[1].checked){
          selected = document.getElementsByClassName('siteInput')[1].value;
        }
        return selected;
      });
      // console.log(devProdHandler);
      if (devProdHandler == 'dev'){
        console.log('TEST PASSED: Development site selected');
      } else if (devProdHandler == 'prod') {
        console.log('TEST PASSED: Production site selected');
      }

      // UNIT TEST: Product Site Upload Verification
      var devProdHandler = await page.evaluate(selected => {
        let devOrProd = $('.siteInput').get();
        devOrProd[1].click();
        if(devOrProd[0].checked){
          selected = document.getElementsByClassName('siteInput')[0].value;
        } else if(devOrProd[1].checked){
          selected = document.getElementsByClassName('siteInput')[1].value;
        }
        return selected;
      });
      // console.log(devProdHandler);
      if (devProdHandler == 'dev'){
        console.log('TEST PASSED: Development site selected');
      } else if (devProdHandler == 'prod') {
        console.log('TEST PASSED: Production site selected');
      }

      var uploadButton = await page.$('.cancel-btn');
      uploadButton.click();
      await page.waitFor(1000);

      // SETUP
      const extensionButtons = await page.$$('#body > #right-panel > #tool-panel > #extension-bar > #extension-bar-menu-container > a');
      for (const button of extensionButtons){
        var text = await page.evaluate(button => button.innerText, button);
        if(text == 'SNAC'){
          button.click();
          break;
        }
      }
      await page.waitFor(1000);

      // UNIT TEST: 'Edit SNAC schema' will take you to the SNAC extension frontend
      var editSchmeaOptionExistTest = false;
      
      menuHandler = await page.$('.menu-container > .menu-item');
      for (const menuOption of menuHandler){
        var text = await page.evaluate(option => option.innerText, menuOption);
        if(text == 'Edit SNAC schema'){
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

      // UNIT TEST: dropdown testing
      var editSchmeaOptionExistTest = false;
      
      dropHandler = await page.$('.selectColumn');
      // for (const menuOption of dropHandler){
      //   var text = await page.evaluate(option => option.innerText, menuOption);
      //   if(text == 'Edit SNAC schema'){
      //     dropHandler.click();
      //     editSchmeaOptionExistTest = true;
      //     break;
      //   }
      // }
      // if(editSchmeaOptionExistTest){
      //   console.log("TEST PASSED: 'Edit SNAC schema' option exists in SNAC extension dropdown.");
      // } else {
      //   console.log("TEST FAILED: 'Edit SNAC schema' option exists in SNAC extension dropdown.");
      // }
      // await page.waitFor(1000);


      /********************** 
       * ISSUES TAB TESTING *
       **********************/ 

      // UNIT TEST: Issues tab exists
      // var issuesExistTest = false;
    
      // const tabHandlers = await page.$$('.main-view-panel-tabs-snac');
      // for (const tab of tabHandlers){
      //   var text = await page.evaluate(tab => tab.innerText, tab);
      //   if (text == 'Issues '){
      //     tab.click();
      //     issuesExistTest = true;
      //     break;
      //   }
      // }
      // if(issuesExistTest){
      //   console.log("TEST PASSED: Issues tab appears after clicking 'Edit SNAC schema'.");
      // } else {
      //   console.log("TEST FAILED: Issues tab appears after clicking Edit SNAC schema.");
      // }
      // await page.waitFor(1000);

      // await page.waitFor(10000);
      // await browser.close();
      // console.log("Browser Closed");
    } catch (err) {
        console.log(err);
        await browser.close();
        console.log("Error: Browser Closed");
    }
  })();
