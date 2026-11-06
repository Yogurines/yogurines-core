import json
a = json.load(open('st3.json')); b = json.load(open('st5.json'))
a.update({k+'_5': v for k, v in b.items()})
s = open('tpl.html').read().replace('/*STATUES*/', json.dumps(a))
open('menu.html', 'w').write(s); print(len(s))
