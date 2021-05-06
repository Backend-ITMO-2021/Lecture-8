# Lecture 8

## Scala Web Server

### Дедлайн 20/05/21 00:01 - 35 баллов

### Дедлайн 27/05/21 00:01 - 10 баллов

Задание выполняется на Scala, весь новый функционал приложения должен быть покрыт тестами. В описании пулреквеста требуется приложить гифку работы приложения (идеальный вариант) или скрины.

1) Реализуйте нумерацию сообщений, возможность отвечать на определённое сообщение и отображение в каскадном виде (
   см. [Lecture-3 п.4](https://github.com/Backend-ITMO-2021/Lecture-3)).
2) Добавьте инпут, позволяющий пользователю фильтровать сообщения и показывать только с совпадающим именем пользователя.
3) Реализуйте API позволяющий запрашивать и добавлять новые сообщения:

| description                               | method | url                   | data                                       | response                                                    |
|-------------------------------------------|--------|-----------------------|--------------------------------------------|-------------------------------------------------------------|
| adds new message, also updates user views | `POST` | `/messages`           | `{username: $USERNAME, message: $MESSAGE, replyTo:? $optionalInt}` |                                                             |
| gets messages from specific user          | `GET`  | `/messages/$username` |                                            | `{messages: ["message 1", "message 2", ...]}`               |
| gets all current messages                 | `GET`  | `/messages`           |                                            | `{messages: [{id: id, username: "user", message: "message", replyTo:? optionalInt}, ...]}` |

Итоговое приложение должно выглядеть примерно так:

![Chat](https://github.com/Backend-ITMO-2021/Lecture-8/blob/main/images/nested-chat.jpg)

![Filtered Chat](https://github.com/Backend-ITMO-2021/Lecture-8/blob/main/images/filtered-chat.jpg)

Docs:

* [Cask](https://com-lihaoyi.github.io/cask/index.html)
* [uTest](https://github.com/com-lihaoyi/utest#getting-started)
* [ScalaTags](https://com-lihaoyi.github.io/scalatags/#BasicExamples)