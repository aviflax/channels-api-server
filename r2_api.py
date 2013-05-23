# -*- coding: utf-8 -*-

from flask import Flask, jsonify, redirect, render_template, request, session, url_for

app = Flask('R2_API')
app.secret_key = 'TEMP-TBD-REPLACE-ME'
app.config.update({'server_name':u'Avi’s R2', 'dev': {'port':3000}})


@app.route('/')
def root():
  return render_template('root.html', server_name=app.config['server_name'])

@app.route('/groups')
def groups():
  return render_template('groups.html', server_name=app.config['server_name'])

@app.route('/groups/<group_id>')
def a_group(group_id):
  return render_template('a_group.html', server_name=app.config['server_name'])

@app.route('/groups/<group_id>/topics')
def topics(group_id):
  return render_template('topics.html', server_name=app.config['server_name'])

@app.route('/groups/<group_id>/topics/<topic_id>')
def a_topic(group_id, topic_id):
  return render_template('a_topic.html', server_name=app.config['server_name'])

@app.route('/groups/<group_id>/topics/<topic_id>/messages')
def messages(group_id, topic_id):
  return render_template('messages.html', server_name=app.config['server_name'])

@app.route('/groups/<group_id>/topics/<topic_id>/messages/<message_id>')
def a_message(group_id, topic_id, message_id):
  return render_template('a_message.html', server_name=app.config['server_name'], message_id=message_id)

if __name__ == '__main__':
    app.run(port=int(app.config['dev']['port']), debug=True)
