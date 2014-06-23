/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.gwt.flux.sample.todo.client.stores;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.gwt.flux.Dispatcher;
import org.jboss.gwt.flux.StoreChangedEvent;
import org.jboss.gwt.flux.meta.Receive;
import org.jboss.gwt.flux.meta.Store;
import org.jboss.gwt.flux.sample.todo.client.TodoServiceAsync;
import org.jboss.gwt.flux.sample.todo.client.actions.ListTodos;
import org.jboss.gwt.flux.sample.todo.client.actions.RemoveTodo;
import org.jboss.gwt.flux.sample.todo.client.actions.SaveTodo;
import org.jboss.gwt.flux.sample.todo.shared.Todo;

@Store
@ApplicationScoped
@SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
public class TodoStore {

    abstract class TodoCallback<T> implements AsyncCallback<T> {

        private final Dispatcher.Channel channel;

        public TodoCallback(final Dispatcher.Channel channel) {
            this.channel = channel;
        }

        @Override
        public void onFailure(final Throwable caught) {
            channel.nack(caught);
        }
    }


    private final List<Todo> todos;
    private final TodoServiceAsync todoService;
    private final EventBus eventBus;

    @Inject
    public TodoStore(final TodoServiceAsync todoService, EventBus eventBus) {
        this.todos = new LinkedList<>();
        this.todoService = todoService;
        this.eventBus = eventBus;
    }

    @Receive
    public void onList(final ListTodos listTodos, final Dispatcher.Channel channel) {
        todoService.list(new TodoCallback<Collection<Todo>>(channel) {
            @Override
            public void onSuccess(final Collection<Todo> result) {
                todos.clear();
                todos.addAll(result);
                channel.ack();
                eventBus.fireEvent(new StoreChangedEvent());
            }
        });
    }

    @Receive
    public void onStore(final SaveTodo saveTodo, final Dispatcher.Channel channel) {
        todoService.save(saveTodo.getTodo(), new TodoCallback<Void>(channel) {
            @Override
            public void onSuccess(final Void result) {
                onList(new ListTodos(), channel);
            }
        });
    }

    @Receive
    public void onRemove(final RemoveTodo removeTodo, final Dispatcher.Channel channel) {
        todoService.delete(removeTodo.getTodo(), new TodoCallback<Void>(channel) {
            @Override
            public void onSuccess(final Void result) {
                onList(new ListTodos(), channel);
            }

            private void fireChanged() {
                eventBus.fireEvent(new StoreChangedEvent());
            }
        });
    }

    public List<Todo> getTodos() {
        return todos;
    }
}