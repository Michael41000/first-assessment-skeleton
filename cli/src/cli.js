import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let previousCommand

const generateTimeStamp = () => {
  const d = new Date()
  const dString = d.toString().split(' ')
  const dayWord = dString[0]
  const month = d.getMonth() + 1
  const day = d.getDate()
  const year = d.getFullYear()
  let hour = d.getHours()
  let night
  if (hour === 12)
  {
    night = true
  }
  else if (hour < 12)
  {
    night = false
  }
  else if (hour > 12)
  {
    hour = hour - 12
    night = true
  }
  const minutes = d.getMinutes();

  let timestamp = dayWord + ' (' + month + '\/' + day + '\/' + year + ')' + ' ' + hour + ':' + minutes
  if (night === true)
  {
    timestamp += 'PM'
  }
  else
  {
    timestamp += 'AM'
  }
  return timestamp
}


cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .description('Connects to a server with given host and port')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    const argsHost = args.host !== undefined ? args.host : 'localhost'
    const argsPort = args.port !== undefined ? args.port : 8080
    const timestamp = generateTimeStamp()
    server = connect({ host: argsHost, port: argsPort }, () => {
      server.write(new Message({ username, command: 'connect', undefined, timestamp}).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      const message = Message.fromJSON(buffer)
      if (message.command === 'connect')
      {
        if (message.error === true)
        {
          this.log(cli.chalk['green']('`' + message.timestamp + ': ' + message.contents + '`'))
        }
        else
        {
          this.log(cli.chalk['green']('`' + message.timestamp + ': <' + message.username + '> has connected`'))
        }
      }
      else if (message.command === 'disconnect')
      {
        this.log(cli.chalk['green']('`' + message.timestamp + ': <' + message.username + '> has disconnected`'))
      }
      else if (message.command === 'echo')
      {
        this.log('`' + message.timestamp + ' <' + message.username + '> ' + '(echo): ' + message.contents + '`')
      }
      else if (message.command === 'broadcast')
      {
        this.log(cli.chalk['yellow']('`' + message.timestamp + ' <' + message.username + '> ' + '(all): ' + message.contents + '`'))
      }
      else if (String(message.command).startsWith('@') === true)
      {
        if (message.error === true)
        {
          this.log(cli.chalk['blue']('`' + message.timestamp + ': ' + message.contents + '`'))
        }
        else
        {
          this.log(cli.chalk['blue']('`' + message.timestamp + ' <' + message.username + '> ' + '(whisper): ' + message.contents + '`'))
        }
      }
      else if (message.command === 'users')
      {
        this.log(cli.chalk['magenta']('`' + message.timestamp + ': ' + 'currently connected users:`' + message.contents))
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input = undefined, callback) {
    const [ command, ...rest ] = input.split(' ')
    let contents = rest.join(' ')
    const timestamp = generateTimeStamp()

    const commands = {
      'disconnect' : 'disconnect from server',
      'echo <message>' : 'repeat message back',
      'users' : 'get list of users connected to server',
      'broadcast <message>' : 'send message to all users',
      '@<username> <message>' : 'send a mess directly to a user',
      '<message>' : 'send message with previously used message command',
      'help' : 'print all commands'
      
    }

    const evaluateCommand = (command) => {
      if (command === 'disconnect') {
        previousCommand = null
        server.end(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'echo') {
        previousCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'users') {
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'broadcast') {
        previousCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (String(command).startsWith('@') === true) {
        previousCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'help') {
        this.log('\n  Commands:\n')
        for (let prop in commands)
        {
          const padding = ' '.repeat(20)
          this.log('    ' + (prop + padding).slice(0, 20) + '\t' + commands[prop])
        }
        this.log()
      } else {
        if (previousCommand)
        {
          contents = (command + ' ' + contents).trim()
          evaluateCommand(previousCommand)
        }
        else
        {
          this.log(`\n  Invalid Command. Showing Help:`)
          evaluateCommand('help')
        }
      }

    }

    evaluateCommand(command);

    

    callback()
  })



