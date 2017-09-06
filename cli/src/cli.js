import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    const argsHost = args.host !== undefined ? args.host : 'localhost'
    const argsPort = args.port !== undefined ? args.port : 8080
    server = connect({ host: argsHost, port: argsPort }, () => {
      server.write(new Message({ username, command: 'connect'}).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')

    const commands = {
      'disconnect' : 'disconnect from server',
      'echo' : 'repeat message back',
      'broadcast' : 'send message to all users',
      'help' : 'print all commands'
    }

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'help') {
      for (let prop in commands)
      {
        this.log(prop + '\t\t' + commands[prop])
      }
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
