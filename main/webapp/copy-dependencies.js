import fs from 'fs';

const FROM_DIR = 'node_modules';
const TO_DIR = 'modules/core/3rdparty';

if (!fs.existsSync(TO_DIR)) {
    fs.mkdirSync(TO_DIR);
    console.log('Directory '+TO_DIR+' created.');
}

try {
    const data = fs.readFileSync('dependencies.json', 'utf8');
    const dependencies = JSON.parse(data);

    const DIRS = dependencies.directories;
    for (const dir of DIRS) {
        const path = TO_DIR+'/'+dir;
        if (!fs.existsSync(path)) {
            fs.mkdirSync(path);
            console.log('Directory '+path+' created.');
        }
    }

    // paths are relative to the FROM_DIR and TO_DIR
    const PATHS = dependencies.files;

    for (const item of PATHS) {
        const from = item.from;
        const fromPath = FROM_DIR + '/' + from;
        let to = item.to;
        if (to === '') {
            to = from;
        }
        const toPath = TO_DIR + '/' + to;
        fs.copyFileSync(fromPath, toPath);
        console.log(fromPath + ' was copied to ' + toPath);
    }

} catch (err) {
    console.log(`Error reading depedencies.json: ${err}`);
    process.exit(-1);
    
}
