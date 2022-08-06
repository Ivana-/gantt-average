# gantt-average

Тестовое задание по следующему описанию:

```
У нас есть интервалы доступности (availability) учителей: :start, :end, :user-id. Вход должен быть интерактивно редактируем прямо тут же, каким-то простым способом (текстовое поле с json/edn).

Задача — визуализировать эти интервалы на следующие сутки/неделю так, чтобы максимально наглядно продемонстрировать покрытие ими всего соответсвующего временного отрезка. Чтобы можно было вглянуть и увидеть интенсивность покрытия, провалы в покрытии.

Из вопросов: насколько хорошо скейлиться будет решение? На 10 учителях/отрезках, 100, 1000?
```

В данной реализации расчет сводного графика для 1000 учителей с 2-10 интервалами каждого занимает порядка 35 милисекунд.

## Development

### Running the App

Start a temporary local web server, build the app with the `dev` profile, and serve the app,
browser test runner and karma test runner with hot reload:

```sh
npm install
npm install chart.js
npx shadow-cljs watch app
```

Please be patient; it may take over 20 seconds to see any output, and over 40 seconds to complete.

When `[:app] Build completed` appears in the output, browse to
[http://localhost:8280/](http://localhost:8280/).

[`shadow-cljs`](https://github.com/thheller/shadow-cljs) will automatically push ClojureScript code
changes to your browser on save. To prevent a few common issues, see
[Hot Reload in ClojureScript: Things to avoid](https://code.thheller.com/blog/shadow-cljs/2019/08/25/hot-reload-in-clojurescript.html#things-to-avoid).

Opening the app in your browser starts a
[ClojureScript browser REPL](https://clojurescript.org/reference/repl#using-the-browser-as-an-evaluation-environment),
to which you may now connect.

## Screenshots

Визуализация актора № 3:

![alt text](https://user-images.githubusercontent.com/10473034/183267975-0946a98b-18f3-41d3-abfb-94fc84a6e38a.png "Actor #3")

Визуализация всех 1000 акторов:

![alt text](https://user-images.githubusercontent.com/10473034/183267980-64250dd4-c1c9-4375-b8cc-a89a2b513172.png "First 1000 actors")
