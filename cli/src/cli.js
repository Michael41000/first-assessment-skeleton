import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let previousMessageCommand

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
        this.log(cli.chalk['green'](message.timestamp + ': ' + message.username + ' has connected'))
      }
      else if (message.command === 'disconnect')
      {
        this.log(cli.chalk['green'](message.timestamp + ': ' + message.username + ' has disconnected'))
      }
      else if (message.command === 'echo')
      {
        this.log(message.timestamp + ' ' + message.username + ' ' + '(echo): ' + message.contents)
      }
      else if (message.command === 'broadcast')
      {
        this.log(cli.chalk['yellow'](message.timestamp + ' ' + message.username + ' ' + '(all): ' + message.contents))
      }
      else if (String(message.command).startsWith('@') === true)
      {
        this.log(cli.chalk['blue'](message.timestamp + ' ' + message.username + ' ' + '(whisper): ' + message.contents))
      }
      else if (message.command === 'users')
      {
        this.log(cli.chalk['magenta'](message.timestamp + ': ' + 'currently connected users:' + message.contents))
      }
      // Just placeholder till everything works
      else {
        this.log(Message.fromJSON(buffer).toString())
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = input.split(' ')
    let contents = rest.join(' ')
    const timestamp = generateTimeStamp()
    
    /*this.log('Command: ' + command)
    this.log('Rest: ' + rest)
    this.log('Contents: ' + contents)*/

    const commands = {
      'disconnect' : 'disconnect from server',
      'echo' : 'repeat message back',
      'users' : 'get list of users connected to server',
      'broadcast' : 'send message to all users',
      '@<username>' : 'send a mess directly to a user',
      '<message>' : 'send message with previously used message command',
      'help' : 'print all commands'
      
    }

    const evaluateCommand = (command) => {
      if (command === 'disconnect') {
        previousMessageCommand = null
        server.end(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'echo') {
        previousMessageCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'users') {
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'broadcast') {
        previousMessageCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (String(command).startsWith('@') === true) {
        previousMessageCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'help') {
        for (let prop in commands)
        {
          this.log(prop + '\t\t' + commands[prop])
        }
      } else {
        if (previousMessageCommand)
        {
          contents = (command + ' ' + contents).trim()
          evaluateCommand(previousMessageCommand)
        }
        else
        {
          this.log(`Command <${command}> was not recognized`)
        }
      }

    }

    evaluateCommand(command);

    

    callback()
  })

