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
      defaultViewport: null,
      devtools: true
    });

    //Testing ApiKey upload verification
    var page = (await browser.pages())[0];
    //await page.goto(`http://127.0.0.1:3333/project?project=1963572809934`);
    await page.goto(`http://127.0.0.1:3333/`);

    var projectURL = await page.evaluate(() => {
      let elements = $('.action-area-tab').toArray()[1];
      $(elements).click(); 
      return document.querySelectorAll('.project-name')[0].href;
    });
    await console.log(projectURL);
    await page.goto(`${projectURL}`);
    await page.waitForSelector('#extension-bar-menu-container');

    await page.evaluate(() => {
      let snac_button = $('#extension-bar-menu-container').toArray()[0].childNodes[0];
      $(snac_button).click();
      let upload = document.getElementsByClassName('menu-item')[2];
      $(upload).click();
      console.log(upload);
    });

    // await expect(apikey).toMatch('newkey')
      
    // const selectElem = await page.$('div[name="menu-container"]');
    // await selectElem.type('Upload edits to SNAC');

    await page.waitFor(100000);
    await browser.close();
    console.log("Browser Closed");
  } catch (err) {
    console.log(err);
    await browser.close();
    console.log("Error: Browser Closed");
  }
  })();