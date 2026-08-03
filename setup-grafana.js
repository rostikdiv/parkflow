const fs = require('fs');
const https = require('https');

https.get('https://grafana.com/api/dashboards/4701/revisions/1/download', (res) => {
    let data = '';
    res.on('data', chunk => { data += chunk; });
    res.on('end', () => {
        let json = data.toString();
        let obj = JSON.parse(json);
        
        delete obj.__inputs;
        delete obj.__requires;
        
        function replaceDatasource(o) {
            if (Array.isArray(o)) {
                o.forEach(replaceDatasource);
            } else if (o !== null && typeof o === 'object') {
                for (let k in o) {
                    if (k === 'datasource') {
                        // Fix for Grafana 10+: datasource is an object
                        o[k] = { type: 'prometheus', uid: 'prometheus' };
                    } else {
                        replaceDatasource(o[k]);
                    }
                }
            }
        }
        replaceDatasource(obj);
        
        if (!fs.existsSync('grafana/provisioning/dashboards')) {
            fs.mkdirSync('grafana/provisioning/dashboards', { recursive: true });
        }
        
        fs.writeFileSync('grafana/provisioning/dashboards/spring-boot.json', JSON.stringify(obj, null, 2), 'utf8');
        console.log('Dashboard successfully written in pure UTF-8');
    });
});
